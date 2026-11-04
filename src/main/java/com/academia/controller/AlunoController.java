package com.academia.controller;

import com.academia.dto.AlunoRequest;
import com.academia.dto.AlunoResponse;
import com.academia.dto.ApiError;
import com.academia.service.AlunoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/alunos")
@Tag(name = "Alunos", description = "Cadastro e consulta de alunos da academia")
public class AlunoController {

    private final AlunoService service;

    public AlunoController(AlunoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cadastrar aluno")
    @ApiResponse(responseCode = "201", description = "Aluno criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "E-mail ou CPF já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<AlunoResponse> criar(@Valid @RequestBody AlunoRequest request) {
        AlunoResponse criado = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);   // 201
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar aluno por id")
    @ApiResponse(responseCode = "200", description = "Aluno encontrado")
    @ApiResponse(responseCode = "404", description = "Aluno não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<AlunoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));   // 200
    }

    @GetMapping
    @Operation(summary = "Listar alunos", description = "Lista paginada. Filtro opcional por nome (contém, sem diferenciar maiúsculas).")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<Page<AlunoResponse>> listar(
            @Parameter(description = "Parte do nome do aluno") @RequestParam(required = false) String nome,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(service.listar(nome, pageable));   // 200
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar aluno")
    @ApiResponse(responseCode = "200", description = "Aluno atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Aluno não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "E-mail ou CPF já pertence a outro aluno",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<AlunoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody AlunoRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));   // 200
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir aluno", description = "Não permite excluir aluno que possua matrículas.")
    @ApiResponse(responseCode = "204", description = "Aluno excluído")
    @ApiResponse(responseCode = "404", description = "Aluno não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Aluno possui matrículas vinculadas",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();   // 204
    }
}
