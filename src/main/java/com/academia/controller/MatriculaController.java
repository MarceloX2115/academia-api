package com.academia.controller;

import com.academia.dto.ApiError;
import com.academia.dto.MatriculaRequest;
import com.academia.dto.MatriculaResponse;
import com.academia.dto.MatriculaUpdateRequest;
import com.academia.model.StatusMatricula;
import com.academia.service.MatriculaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/matriculas")
@Tag(name = "Matrículas", description = "Matrículas de alunos em planos, com controle de vencimento")
public class MatriculaController {

    private final MatriculaService service;

    public MatriculaController(MatriculaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Realizar matrícula",
            description = "Calcula automaticamente o vencimento (início + duração do plano) e o valor total. "
                    + "Impede matrícula duplicada e matrícula em plano inativo.")
    @ApiResponse(responseCode = "201", description = "Matrícula criada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Aluno ou plano não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Matrícula duplicada, plano inativo ou data de início inválida",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<MatriculaResponse> criar(@Valid @RequestBody MatriculaRequest request) {
        MatriculaResponse criada = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);   // 201
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar matrícula por id")
    @ApiResponse(responseCode = "200", description = "Matrícula encontrada")
    @ApiResponse(responseCode = "404", description = "Matrícula não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<MatriculaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));   // 200
    }

    @GetMapping
    @Operation(summary = "Listar matrículas",
            description = "Lista paginada. Filtros opcionais: nome do aluno (contém) e status da matrícula.")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    @ApiResponse(responseCode = "400", description = "Status inválido (use ATIVA, VENCIDA ou CANCELADA)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Page<MatriculaResponse>> listar(
            @Parameter(description = "Parte do nome do aluno") @RequestParam(required = false) String nomeAluno,
            @Parameter(description = "Status da matrícula") @RequestParam(required = false) StatusMatricula status,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(service.listar(nomeAluno, status, pageable));   // 200
    }

    @GetMapping("/vencendo")
    @Operation(summary = "Listar matrículas que vencem em breve",
            description = "Matrículas ativas com vencimento entre hoje e hoje + N dias (padrão 7).")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    @ApiResponse(responseCode = "400", description = "Número de dias inválido (1 a 365)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<List<MatriculaResponse>> vencendo(
            @Parameter(description = "Janela em dias a partir de hoje")
            @RequestParam(defaultValue = "7")
            @Min(value = 1, message = "O número de dias deve ser no mínimo 1")
            @Max(value = 365, message = "O número de dias deve ser no máximo 365") int dias) {
        return ResponseEntity.ok(service.listarVencendo(dias));   // 200
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar matrícula", description = "Permite trocar o plano e/ou a data de início. Recalcula vencimento e valor.")
    @ApiResponse(responseCode = "200", description = "Matrícula atualizada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Matrícula ou plano não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Matrícula cancelada, plano inativo, duplicidade ou data inválida",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<MatriculaResponse> atualizar(@PathVariable Long id,
                                                       @Valid @RequestBody MatriculaUpdateRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));   // 200
    }

    @PatchMapping("/{id}/renovar")
    @Operation(summary = "Renovar matrícula",
            description = "Estende o vencimento pela duração do plano (a partir de hoje, se já estiver vencida).")
    @ApiResponse(responseCode = "200", description = "Matrícula renovada")
    @ApiResponse(responseCode = "404", description = "Matrícula não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Matrícula cancelada, plano inativo ou duplicidade",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<MatriculaResponse> renovar(@PathVariable Long id) {
        return ResponseEntity.ok(service.renovar(id));   // 200
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar matrícula", description = "Somente matrículas ativas podem ser canceladas.")
    @ApiResponse(responseCode = "200", description = "Matrícula cancelada")
    @ApiResponse(responseCode = "404", description = "Matrícula não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Matrícula não está ativa",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<MatriculaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelar(id));   // 200
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir matrícula", description = "Matrícula ativa deve ser cancelada antes de ser excluída.")
    @ApiResponse(responseCode = "204", description = "Matrícula excluída")
    @ApiResponse(responseCode = "404", description = "Matrícula não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Matrícula ainda está ativa",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();   // 204
    }
}
