package com.academia.dto;

import java.math.BigDecimal;

public record PlanoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal valorMensal,
        Integer duracaoMeses,
        boolean ativo
) {
}
