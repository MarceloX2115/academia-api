package com.academia.dto;

import com.academia.model.StatusMatricula;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MatriculaResponse(
        Long id,
        Long alunoId,
        String alunoNome,
        Long planoId,
        String planoNome,
        LocalDate dataInicio,
        LocalDate dataVencimento,
        BigDecimal valorTotal,
        StatusMatricula status,
        long diasParaVencimento
) {
}
