package com.academia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Dados para realizar uma matrícula")
public record MatriculaRequest(

        @Schema(example = "1")
        @NotNull(message = "O id do aluno é obrigatório")
        Long alunoId,

        @Schema(example = "1")
        @NotNull(message = "O id do plano é obrigatório")
        Long planoId,

        @Schema(example = "2026-11-03", description = "Opcional. Se omitida, usa a data de hoje")
        LocalDate dataInicio
) {
}
