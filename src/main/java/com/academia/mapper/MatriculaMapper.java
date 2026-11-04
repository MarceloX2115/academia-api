package com.academia.mapper;

import com.academia.dto.MatriculaResponse;
import com.academia.model.Matricula;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Conversão manual entre a entidade Matricula e seu DTO de resposta.
 */
@Component
public class MatriculaMapper {

    public MatriculaResponse toResponse(Matricula m) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), m.getDataVencimento());
        return new MatriculaResponse(
                m.getId(),
                m.getAluno().getId(),
                m.getAluno().getNome(),
                m.getPlano().getId(),
                m.getPlano().getNome(),
                m.getDataInicio(),
                m.getDataVencimento(),
                m.getValorTotal(),
                m.getStatus(),
                dias);
    }
}
