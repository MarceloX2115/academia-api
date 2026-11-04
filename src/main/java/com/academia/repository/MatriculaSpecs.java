package com.academia.repository;

import com.academia.model.Matricula;
import com.academia.model.StatusMatricula;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros dinâmicos para consulta de matrículas.
 */
public final class MatriculaSpecs {

    private MatriculaSpecs() {
    }

    public static Specification<Matricula> nomeAlunoContem(String nome) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("aluno").get("nome")),
                "%" + nome.trim().toLowerCase() + "%");
    }

    public static Specification<Matricula> comStatus(StatusMatricula status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
