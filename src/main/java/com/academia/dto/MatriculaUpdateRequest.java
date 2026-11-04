package com.academia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Dados para atualizar uma matrícula existente (troca de plano e/ou data de início)")
public record MatriculaUpdateRequest(

        @Schema(example = "2")
        @NotNull(message = "O id do plano é obrigatório")
        Long planoId,

        @Schema(example = "2026-11-10", description = "Opcional. Se omitida, mantém a data de início atual")
        LocalDate dataInicio
) {
}
