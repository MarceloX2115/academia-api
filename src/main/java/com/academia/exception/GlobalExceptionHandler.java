package com.academia.exception;

import com.academia.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404 - registro inexistente
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> handleNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    // 409 - violação de regra de negócio
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiError> handleRegraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        log.warn("Regra de negócio violada: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    // 409 - restrições do banco (ex.: campo único) que escaparam das regras do Service
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade no banco: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Operação viola uma restrição de integridade dos dados.", request, List.of());
    }

    // 400 - dados do corpo da requisição inválidos (Bean Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos.", request, detalhes);
    }

    // 400 - validação de parâmetros simples (@RequestParam com @Min/@Max etc.)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleValidacaoParametros(HandlerMethodValidationException ex, HttpServletRequest request) {
        List<String> detalhes = new ArrayList<>();
        ex.getParameterValidationResults().forEach(r ->
                r.getResolvableErrors().forEach(e -> detalhes.add(e.getDefaultMessage())));
        return build(HttpStatus.BAD_REQUEST, "Parâmetros inválidos.", request, detalhes);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<String> detalhes = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Parâmetros inválidos.", request, detalhes);
    }

    // 400 - JSON malformado, data em formato errado, enum inexistente no corpo etc.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido ou malformado. Verifique o JSON e o formato dos campos (ex.: datas AAAA-MM-DD).",
                request, List.of());
    }

    // 400 - parâmetro de URL com tipo/valor inválido (ex.: status=XYZ ou id=abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String mensagem = "Valor inválido para o parâmetro '" + ex.getName() + "': " + ex.getValue();
        return build(HttpStatus.BAD_REQUEST, mensagem, request, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleParametroAusente(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + ex.getParameterName(), request, List.of());
    }

    // 500 - qualquer erro inesperado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenerico(Exception ex, HttpServletRequest request) {
        // Erros HTTP padrão do Spring (404 de rota, 405 método não permitido, 415 etc.) mantêm o seu status.
        if (ex instanceof ErrorResponse erroHttp) {
            HttpStatus status = HttpStatus.valueOf(erroHttp.getStatusCode().value());
            return build(status, erroHttp.getBody().getDetail(), request, List.of());
        }
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor.", request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String mensagem, HttpServletRequest request, List<String> detalhes) {
        ApiError erro = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI(),
                detalhes);
        return ResponseEntity.status(status).body(erro);
    }
}
