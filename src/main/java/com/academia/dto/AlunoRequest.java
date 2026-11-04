package com.academia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Dados para cadastro/atualização de aluno")
public record AlunoRequest(

        @Schema(example = "Maria da Silva")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        String nome,

        @Schema(example = "maria@email.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @Schema(example = "12345678901", description = "CPF com 11 dígitos, somente números")
        @NotBlank(message = "O CPF é obrigatório")
        @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos")
        String cpf,

        @Schema(example = "(86) 99999-0000")
        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
        String telefone,

        @Schema(example = "1998-05-20")
        @Past(message = "A data de nascimento deve estar no passado")
        LocalDate dataNascimento
) {
}
