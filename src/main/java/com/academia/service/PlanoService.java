package com.academia.service;

import com.academia.dto.PlanoRequest;
import com.academia.dto.PlanoResponse;
import com.academia.exception.RecursoNaoEncontradoException;
import com.academia.exception.RegraNegocioException;
import com.academia.mapper.PlanoMapper;
import com.academia.model.Plano;
import com.academia.repository.MatriculaRepository;
import com.academia.repository.PlanoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanoService {

    private static final Logger log = LoggerFactory.getLogger(PlanoService.class);

    private final PlanoRepository planoRepository;
    private final MatriculaRepository matriculaRepository;
    private final PlanoMapper mapper;

    public PlanoService(PlanoRepository planoRepository, MatriculaRepository matriculaRepository, PlanoMapper mapper) {
        this.planoRepository = planoRepository;
        this.matriculaRepository = matriculaRepository;
        this.mapper = mapper;
    }

    @Transactional
    public PlanoResponse criar(PlanoRequest request) {
        if (planoRepository.existsByNomeIgnoreCase(request.nome().trim())) {
            throw new RegraNegocioException("Já existe um plano com o nome '" + request.nome() + "'");
        }
        Plano salvo = planoRepository.save(mapper.toEntity(request));
        log.info("Plano cadastrado: id={}, nome={}", salvo.getId(), salvo.getNome());
        return mapper.toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public PlanoResponse buscarPorId(Long id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public Page<PlanoResponse> listar(Boolean ativo, Pageable pageable) {
        Page<Plano> pagina = (ativo == null)
                ? planoRepository.findAll(pageable)
                : planoRepository.findByAtivo(ativo, pageable);
        return pagina.map(mapper::toResponse);
    }

    @Transactional
    public PlanoResponse atualizar(Long id, PlanoRequest request) {
        Plano plano = buscarEntidade(id);
        if (planoRepository.existsByNomeIgnoreCaseAndIdNot(request.nome().trim(), id)) {
            throw new RegraNegocioException("Já existe outro plano com o nome '" + request.nome() + "'");
        }
        mapper.updateEntity(plano, request);
        log.info("Plano atualizado: id={}, ativo={}", id, plano.isAtivo());
        return mapper.toResponse(planoRepository.save(plano));
    }

    @Transactional
    public void excluir(Long id) {
        Plano plano = buscarEntidade(id);
        // Regra: não excluir plano que possui matrículas (sugere-se inativá-lo).
        if (matriculaRepository.existsByPlanoId(id)) {
            throw new RegraNegocioException("Não é possível excluir o plano '" + plano.getNome()
                    + "' pois existem matrículas vinculadas. Inative o plano em vez de excluí-lo.");
        }
        planoRepository.delete(plano);
        log.info("Plano excluído: id={}", id);
    }

    private Plano buscarEntidade(Long id) {
        return planoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano não encontrado com id " + id));
    }
}
