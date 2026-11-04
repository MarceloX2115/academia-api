package com.academia.controller;

import com.academia.dto.ApiError;
import com.academia.dto.PlanoRequest;
import com.academia.dto.PlanoResponse;
import com.academia.service.PlanoService;
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
@RequestMapping("/api/planos")
@Tag(name = "Planos", description = "Cadastro e consulta de planos da academia")
public class PlanoController {

    private final PlanoService service;

    public PlanoController(PlanoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cadastrar plano")
    @ApiResponse(responseCode = "201", description = "Plano criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Já existe plano com esse nome",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<PlanoResponse> criar(@Valid @RequestBody PlanoRequest request) {
        PlanoResponse criado = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);   // 201
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar plano por id")
    @ApiResponse(responseCode = "200", description = "Plano encontrado")
    @ApiResponse(responseCode = "404", description = "Plano não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<PlanoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));   // 200
    }

    @GetMapping
    @Operation(summary = "Listar planos", description = "Lista paginada. Filtro opcional por situação (ativo=true/false).")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<Page<PlanoResponse>> listar(
            @Parameter(description = "Filtrar por planos ativos (true) ou inativos (false)")
            @RequestParam(required = false) Boolean ativo,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(service.listar(ativo, pageable));   // 200
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar plano", description = "Use o campo 'ativo' para ativar ou inativar o plano.")
    @ApiResponse(responseCode = "200", description = "Plano atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Plano não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Já existe outro plano com esse nome",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<PlanoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody PlanoRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));   // 200
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir plano", description = "Não permite excluir plano que possua matrículas.")
    @ApiResponse(responseCode = "204", description = "Plano excluído")
    @ApiResponse(responseCode = "404", description = "Plano não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Plano possui matrículas vinculadas",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();   // 204
    }
}
