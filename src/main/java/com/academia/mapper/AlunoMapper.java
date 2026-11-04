package com.academia.mapper;

import com.academia.dto.AlunoRequest;
import com.academia.dto.AlunoResponse;
import com.academia.model.Aluno;
import org.springframework.stereotype.Component;

@Component
public class AlunoMapper {

    public Aluno toEntity(AlunoRequest request) {
        Aluno aluno = new Aluno();
        updateEntity(aluno, request);
        return aluno;
    }

    public void updateEntity(Aluno aluno, AlunoRequest request) {
        aluno.setNome(request.nome().trim());
        aluno.setEmail(request.email().trim().toLowerCase());
        aluno.setCpf(request.cpf());
        aluno.setTelefone(request.telefone());
        aluno.setDataNascimento(request.dataNascimento());
    }

    public AlunoResponse toResponse(Aluno aluno) {
        return new AlunoResponse(
                aluno.getId(),
                aluno.getNome(),
                aluno.getEmail(),
                aluno.getCpf(),
                aluno.getTelefone(),
                aluno.getDataNascimento());
    }
}
