package com.academia.repository;

import com.academia.model.Matricula;
import com.academia.model.StatusMatricula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Long>, JpaSpecificationExecutor<Matricula> {

    // Carrega aluno e plano junto com a matrícula (evita consultas extras N+1).
    @Override
    @EntityGraph(attributePaths = {"aluno", "plano"})
    Page<Matricula> findAll(Specification<Matricula> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"aluno", "plano"})
    Optional<Matricula> findWithAlunoAndPlanoById(Long id);

    @EntityGraph(attributePaths = {"aluno", "plano"})
    List<Matricula> findByStatusAndDataVencimentoBetweenOrderByDataVencimentoAsc(
            StatusMatricula status, LocalDate inicio, LocalDate fim);

    // Regra: impedir matrícula duplicada (mesmo aluno + mesmo plano com matrícula ativa).
    boolean existsByAlunoIdAndPlanoIdAndStatus(Long alunoId, Long planoId, StatusMatricula status);

    boolean existsByAlunoIdAndPlanoIdAndStatusAndIdNot(Long alunoId, Long planoId, StatusMatricula status, Long id);

    // Regra: impedir exclusão de aluno/plano que possuem matrículas.
    boolean existsByAlunoId(Long alunoId);

    boolean existsByPlanoId(Long planoId);

    // Regra: controle de vencimento. Marca como VENCIDA toda matrícula ATIVA com data de vencimento passada.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Matricula m set m.status = com.academia.model.StatusMatricula.VENCIDA "
            + "where m.status = com.academia.model.StatusMatricula.ATIVA and m.dataVencimento < :hoje")
    int marcarVencidas(@Param("hoje") LocalDate hoje);
}
