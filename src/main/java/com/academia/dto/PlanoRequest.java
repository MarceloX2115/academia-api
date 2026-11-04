package com.academia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados para cadastro/atualização de plano")
public record PlanoRequest(

        @Schema(example = "Trimestral")
        @NotBlank(message = "O nome do plano é obrigatório")
        @Size(min = 2, max = 80, message = "O nome deve ter entre 2 e 80 caracteres")
        String nome,

        @Schema(example = "Acesso livre a todas as modalidades")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao,

        @Schema(example = "99.90")
        @NotNull(message = "O valor mensal é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor mensal deve ser maior que zero")
        BigDecimal valorMensal,

        @Schema(example = "3", description = "Duração do plano em meses (1 a 36)")
        @NotNull(message = "A duração em meses é obrigatória")
        @Min(value = 1, message = "A duração mínima é 1 mês")
        @Max(value = 36, message = "A duração máxima é 36 meses")
        Integer duracaoMeses,

        @Schema(example = "true", description = "Se omitido, o plano é criado como ativo")
        Boolean ativo
) {
}
