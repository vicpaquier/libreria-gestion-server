package com.americana.bibliotecaserver.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> manejarValidacion(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        var errorCampo = exception.getBindingResult().getFieldErrors().getFirst();
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "VALIDACION",
                errorCampo.getDefaultMessage(),
                errorCampo.getField(),
                request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> manejarCuerpoInvalido(HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "SOLICITUD_INVALIDA",
                "El cuerpo de la solicitud no es válido",
                null,
                request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> manejarParametroDeTipoInvalido(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "SOLICITUD_INVALIDA",
                "El parámetro no tiene un valor válido",
                exception.getName(),
                request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> manejarRestriccionDeParametro(
            ConstraintViolationException exception, HttpServletRequest request) {
        var violacion = exception.getConstraintViolations().iterator().next();
        var ruta = violacion.getPropertyPath().toString();
        var campo = ruta.substring(ruta.lastIndexOf('.') + 1);
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "VALIDACION",
                violacion.getMessage(),
                campo,
                request.getRequestURI());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> manejarValidacionDeMetodo(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        var resultado = exception.getParameterValidationResults().getFirst();
        var error = resultado.getResolvableErrors().getFirst();
        var campo = error instanceof FieldError errorCampo
                ? errorCampo.getField()
                : resultado.getMethodParameter().getParameterName();
        return respuesta(
                HttpStatus.BAD_REQUEST,
                "VALIDACION",
                error.getDefaultMessage(),
                campo,
                request.getRequestURI());
    }

    @ExceptionHandler(ConflictoUnicidadException.class)
    ResponseEntity<ApiError> manejarConflicto(ConflictoUnicidadException exception,
            HttpServletRequest request) {
        return respuesta(
                HttpStatus.CONFLICT,
                exception.getCodigo(),
                exception.getMessage(),
                exception.getCampo(),
                request.getRequestURI());
    }

    @ExceptionHandler(ConflictoNegocioException.class)
    ResponseEntity<ApiError> manejarConflictoDeNegocio(ConflictoNegocioException exception,
            HttpServletRequest request) {
        return respuesta(
                HttpStatus.CONFLICT,
                exception.getCodigo(),
                exception.getMessage(),
                exception.getCampo(),
                request.getRequestURI());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ApiError> manejarRecursoNoEncontrado(RecursoNoEncontradoException exception,
            HttpServletRequest request) {
        return respuesta(
                HttpStatus.NOT_FOUND,
                "RECURSO_NO_ENCONTRADO",
                exception.getMessage(),
                null,
                request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> manejarErrorInterno(Exception exception, HttpServletRequest request) {
        LOGGER.error("Error no controlado al procesar {}", request.getRequestURI(), exception);
        return respuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERROR_INTERNO",
                "No fue posible completar la solicitud",
                null,
                request.getRequestURI());
    }

    private ResponseEntity<ApiError> respuesta(HttpStatus estado, String codigo, String mensaje,
            String campo, String path) {
        return ResponseEntity.status(estado).body(new ApiError(
                OffsetDateTime.now(), estado.value(), codigo, mensaje, campo, path));
    }
}
