package com.academia.repository;

import com.academia.model.Aluno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    // Filtro por nome (contém, sem diferenciar maiúsculas/minúsculas).
    Page<Aluno> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);
}
