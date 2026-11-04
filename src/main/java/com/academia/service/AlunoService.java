package com.academia.service;

import com.academia.dto.AlunoRequest;
import com.academia.dto.AlunoResponse;
import com.academia.exception.RecursoNaoEncontradoException;
import com.academia.exception.RegraNegocioException;
import com.academia.mapper.AlunoMapper;
import com.academia.model.Aluno;
import com.academia.repository.AlunoRepository;
import com.academia.repository.MatriculaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlunoService {

    private static final Logger log = LoggerFactory.getLogger(AlunoService.class);

    private final AlunoRepository alunoRepository;
    private final MatriculaRepository matriculaRepository;
    private final AlunoMapper mapper;

    public AlunoService(AlunoRepository alunoRepository, MatriculaRepository matriculaRepository, AlunoMapper mapper) {
        this.alunoRepository = alunoRepository;
        this.matriculaRepository = matriculaRepository;
        this.mapper = mapper;
    }

    @Transactional
    public AlunoResponse criar(AlunoRequest request) {
        // Regra: não permitir aluno com e-mail ou CPF já cadastrados.
        if (alunoRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new RegraNegocioException("Já existe um aluno cadastrado com o e-mail " + request.email());
        }
        if (alunoRepository.existsByCpf(request.cpf())) {
            throw new RegraNegocioException("Já existe um aluno cadastrado com o CPF " + request.cpf());
        }
        Aluno salvo = alunoRepository.save(mapper.toEntity(request));
        log.info("Aluno cadastrado: id={}, nome={}", salvo.getId(), salvo.getNome());
        return mapper.toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public AlunoResponse buscarPorId(Long id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public Page<AlunoResponse> listar(String nome, Pageable pageable) {
        Page<Aluno> pagina = (nome == null || nome.isBlank())
                ? alunoRepository.findAll(pageable)
                : alunoRepository.findByNomeContainingIgnoreCase(nome.trim(), pageable);
        return pagina.map(mapper::toResponse);
    }

    @Transactional
    public AlunoResponse atualizar(Long id, AlunoRequest request) {
        Aluno aluno = buscarEntidade(id);
        if (alunoRepository.existsByEmailIgnoreCaseAndIdNot(request.email().trim(), id)) {
            throw new RegraNegocioException("Já existe outro aluno cadastrado com o e-mail " + request.email());
        }
        if (alunoRepository.existsByCpfAndIdNot(request.cpf(), id)) {
            throw new RegraNegocioException("Já existe outro aluno cadastrado com o CPF " + request.cpf());
        }
        mapper.updateEntity(aluno, request);
        log.info("Aluno atualizado: id={}", id);
        return mapper.toResponse(alunoRepository.save(aluno));
    }

    @Transactional
    public void excluir(Long id) {
        Aluno aluno = buscarEntidade(id);
        // Regra: não excluir aluno que possui matrículas (preserva o histórico).
        if (matriculaRepository.existsByAlunoId(id)) {
            throw new RegraNegocioException("Não é possível excluir o aluno '" + aluno.getNome()
                    + "' pois ele possui matrículas vinculadas.");
        }
        alunoRepository.delete(aluno);
        log.info("Aluno excluído: id={}", id);
    }

    private Aluno buscarEntidade(Long id) {
        return alunoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado com id " + id));
    }
}
