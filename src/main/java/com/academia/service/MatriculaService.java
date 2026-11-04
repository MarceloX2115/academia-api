package com.academia.service;

import com.academia.dto.MatriculaRequest;
import com.academia.dto.MatriculaResponse;
import com.academia.dto.MatriculaUpdateRequest;
import com.academia.exception.RecursoNaoEncontradoException;
import com.academia.exception.RegraNegocioException;
import com.academia.mapper.MatriculaMapper;
import com.academia.model.Aluno;
import com.academia.model.Matricula;
import com.academia.model.Plano;
import com.academia.model.StatusMatricula;
import com.academia.repository.AlunoRepository;
import com.academia.repository.MatriculaRepository;
import com.academia.repository.MatriculaSpecs;
import com.academia.repository.PlanoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Concentra as regras de negócio das matrículas:
 *  1. Impedir matrícula duplicada (mesmo aluno + mesmo plano com matrícula ativa).
 *  2. Impedir matrícula em plano inativo.
 *  3. Controlar vencimento (data calculada pela duração do plano; status VENCIDA automático; renovação).
 *  4. Calcular automaticamente o valor total (valorMensal x duracaoMeses).
 *  5. Validar a data de início (não pode ser passada nem mais de 30 dias à frente).
 *  6. Não permitir exclusão de matrícula ativa (é preciso cancelar antes) nem alteração de matrícula cancelada.
 */
@Service
public class MatriculaService {

    private static final Logger log = LoggerFactory.getLogger(MatriculaService.class);
    private static final int LIMITE_DIAS_FUTURO = 30;

    private final MatriculaRepository matriculaRepository;
    private final AlunoRepository alunoRepository;
    private final PlanoRepository planoRepository;
    private final MatriculaMapper mapper;

    public MatriculaService(MatriculaRepository matriculaRepository,
                            AlunoRepository alunoRepository,
                            PlanoRepository planoRepository,
                            MatriculaMapper mapper) {
        this.matriculaRepository = matriculaRepository;
        this.alunoRepository = alunoRepository;
        this.planoRepository = planoRepository;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ CREATE

    @Transactional
    public MatriculaResponse criar(MatriculaRequest request) {
        atualizarVencidas();

        Aluno aluno = alunoRepository.findById(request.alunoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado com id " + request.alunoId()));
        Plano plano = planoRepository.findById(request.planoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano não encontrado com id " + request.planoId()));

        LocalDate inicio = request.dataInicio() != null ? request.dataInicio() : LocalDate.now();
        validarDataInicio(inicio);
        validarPlanoAtivo(plano);                                  // regra 2
        if (matriculaRepository.existsByAlunoIdAndPlanoIdAndStatus(aluno.getId(), plano.getId(), StatusMatricula.ATIVA)) {
            throw new RegraNegocioException(mensagemDuplicada(aluno, plano));   // regra 1
        }

        Matricula matricula = new Matricula();
        matricula.setAluno(aluno);
        matricula.setPlano(plano);
        aplicarPlano(matricula, plano, inicio);                    // regras 3 e 4
        matricula.setStatus(StatusMatricula.ATIVA);

        Matricula salva = matriculaRepository.save(matricula);
        log.info("Matrícula criada: id={}, aluno={}, plano={}, vencimento={}",
                salva.getId(), aluno.getId(), plano.getId(), salva.getDataVencimento());
        return mapper.toResponse(salva);
    }

    // -------------------------------------------------------------------- READ

    @Transactional
    public MatriculaResponse buscarPorId(Long id) {
        atualizarVencidas();
        return mapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public Page<MatriculaResponse> listar(String nomeAluno, StatusMatricula status, Pageable pageable) {
        atualizarVencidas();   // garante que o filtro por status reflita o vencimento real

        Specification<Matricula> spec = Specification.where(null);
        if (nomeAluno != null && !nomeAluno.isBlank()) {
            spec = spec.and(MatriculaSpecs.nomeAlunoContem(nomeAluno));
        }
        if (status != null) {
            spec = spec.and(MatriculaSpecs.comStatus(status));
        }
        return matriculaRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    /** Matrículas ativas que vencem nos próximos N dias. */
    @Transactional
    public List<MatriculaResponse> listarVencendo(int dias) {
        atualizarVencidas();
        LocalDate hoje = LocalDate.now();
        return matriculaRepository
                .findByStatusAndDataVencimentoBetweenOrderByDataVencimentoAsc(
                        StatusMatricula.ATIVA, hoje, hoje.plusDays(dias))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------ UPDATE

    @Transactional
    public MatriculaResponse atualizar(Long id, MatriculaUpdateRequest request) {
        atualizarVencidas();
        Matricula matricula = buscarEntidade(id);

        if (matricula.getStatus() == StatusMatricula.CANCELADA) {
            throw new RegraNegocioException("Matrícula cancelada não pode ser alterada.");
        }

        Plano plano = matricula.getPlano();
        if (!plano.getId().equals(request.planoId())) {
            plano = planoRepository.findById(request.planoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Plano não encontrado com id " + request.planoId()));
            validarPlanoAtivo(plano);   // só exige plano ativo quando o plano está sendo trocado
        }

        LocalDate inicio = request.dataInicio() != null ? request.dataInicio() : matricula.getDataInicio();
        if (!inicio.equals(matricula.getDataInicio())) {
            validarDataInicio(inicio);
        }

        aplicarPlano(matricula, plano, inicio);
        matricula.setPlano(plano);
        matricula.setStatus(matricula.getDataVencimento().isBefore(LocalDate.now())
                ? StatusMatricula.VENCIDA : StatusMatricula.ATIVA);

        if (matricula.getStatus() == StatusMatricula.ATIVA
                && matriculaRepository.existsByAlunoIdAndPlanoIdAndStatusAndIdNot(
                matricula.getAluno().getId(), plano.getId(), StatusMatricula.ATIVA, id)) {
            throw new RegraNegocioException(mensagemDuplicada(matricula.getAluno(), plano));
        }

        log.info("Matrícula atualizada: id={}, plano={}, vencimento={}", id, plano.getId(), matricula.getDataVencimento());
        return mapper.toResponse(matriculaRepository.save(matricula));
    }

    /** Renova a matrícula: estende o vencimento pela duração do plano (a partir de hoje se já estiver vencida). */
    @Transactional
    public MatriculaResponse renovar(Long id) {
        atualizarVencidas();
        Matricula matricula = buscarEntidade(id);

        if (matricula.getStatus() == StatusMatricula.CANCELADA) {
            throw new RegraNegocioException("Matrícula cancelada não pode ser renovada. Faça uma nova matrícula.");
        }
        Plano plano = matricula.getPlano();
        validarPlanoAtivo(plano);

        // Se já estava vencida, não pode existir outra matrícula ativa igual para o mesmo aluno/plano.
        if (matriculaRepository.existsByAlunoIdAndPlanoIdAndStatusAndIdNot(
                matricula.getAluno().getId(), plano.getId(), StatusMatricula.ATIVA, id)) {
            throw new RegraNegocioException(mensagemDuplicada(matricula.getAluno(), plano));
        }

        LocalDate hoje = LocalDate.now();
        LocalDate base = matricula.getDataVencimento().isAfter(hoje) ? matricula.getDataVencimento() : hoje;
        matricula.setDataVencimento(base.plusMonths(plano.getDuracaoMeses()));
        matricula.setValorTotal(matricula.getValorTotal().add(calcularValorTotal(plano)));
        matricula.setStatus(StatusMatricula.ATIVA);

        log.info("Matrícula renovada: id={}, novo vencimento={}", id, matricula.getDataVencimento());
        return mapper.toResponse(matriculaRepository.save(matricula));
    }

    @Transactional
    public MatriculaResponse cancelar(Long id) {
        atualizarVencidas();
        Matricula matricula = buscarEntidade(id);

        if (matricula.getStatus() != StatusMatricula.ATIVA) {
            throw new RegraNegocioException("Apenas matrículas ativas podem ser canceladas (situação atual: "
                    + matricula.getStatus() + ").");
        }
        matricula.setStatus(StatusMatricula.CANCELADA);
        log.info("Matrícula cancelada: id={}", id);
        return mapper.toResponse(matriculaRepository.save(matricula));
    }

    // ------------------------------------------------------------------ DELETE

    @Transactional
    public void excluir(Long id) {
        atualizarVencidas();
        Matricula matricula = buscarEntidade(id);

        if (matricula.getStatus() == StatusMatricula.ATIVA) {
            throw new RegraNegocioException("Não é possível excluir uma matrícula ativa. Cancele-a antes de excluir.");
        }
        matriculaRepository.delete(matricula);
        log.info("Matrícula excluída: id={}", id);
    }

    // ------------------------------------------------------- CONTROLE DE VENCIMENTO

    /** Marca como VENCIDA toda matrícula ATIVA cujo vencimento já passou. */
    @Transactional
    public int atualizarVencidas() {
        int total = matriculaRepository.marcarVencidas(LocalDate.now());
        if (total > 0) {
            log.info("{} matrícula(s) marcada(s) como VENCIDA", total);
        }
        return total;
    }

    /** Rotina automática: todos os dias às 00:05 atualiza as matrículas vencidas. */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void rotinaDiariaDeVencimento() {
        atualizarVencidas();
    }

    // --------------------------------------------------------- MÉTODOS AUXILIARES

    private Matricula buscarEntidade(Long id) {
        return matriculaRepository.findWithAlunoAndPlanoById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula não encontrada com id " + id));
    }

    private void validarPlanoAtivo(Plano plano) {
        if (!plano.isAtivo()) {
            throw new RegraNegocioException("O plano '" + plano.getNome() + "' está inativo e não aceita matrículas.");
        }
    }

    private void validarDataInicio(LocalDate inicio) {
        LocalDate hoje = LocalDate.now();
        if (inicio.isBefore(hoje)) {
            throw new RegraNegocioException("A data de início não pode estar no passado.");
        }
        if (inicio.isAfter(hoje.plusDays(LIMITE_DIAS_FUTURO))) {
            throw new RegraNegocioException("A data de início não pode ser superior a "
                    + LIMITE_DIAS_FUTURO + " dias a partir de hoje.");
        }
    }

    /** Define início, vencimento e valor total a partir do plano. */
    private void aplicarPlano(Matricula matricula, Plano plano, LocalDate inicio) {
        matricula.setDataInicio(inicio);
        matricula.setDataVencimento(inicio.plusMonths(plano.getDuracaoMeses()));
        matricula.setValorTotal(calcularValorTotal(plano));
    }

    private BigDecimal calcularValorTotal(Plano plano) {
        return plano.getValorMensal().multiply(BigDecimal.valueOf(plano.getDuracaoMeses()));
    }

    private String mensagemDuplicada(Aluno aluno, Plano plano) {
        return "O aluno '" + aluno.getNome() + "' já possui uma matrícula ativa no plano '" + plano.getNome() + "'.";
    }
}
