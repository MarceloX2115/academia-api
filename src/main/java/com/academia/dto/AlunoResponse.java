package com.academia.dto;

import java.time.LocalDate;

public record AlunoResponse(
        Long id,
        String nome,
        String email,
        String cpf,
        String telefone,
        LocalDate dataNascimento
) {
}
