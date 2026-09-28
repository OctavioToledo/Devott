package com.devott.compartido.errores;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.List;

/**
 * Convierte las excepciones en respuestas ProblemDetail (RFC 9457) con textos en español.
 * Los errores de validación agregan la propiedad "errores" con un mensaje por campo.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    public record ErrorDeCampo(String campo, String mensaje) {
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException ex) {
        ProblemDetail problema = problema(HttpStatus.CONFLICT, ex.getMessage());
        if (ex.getCampo() != null) {
            problema.setProperty("errores", List.of(new ErrorDeCampo(ex.getCampo(), ex.getMessage())));
        }
        return problema;
    }

    @ExceptionHandler(DatosInvalidosException.class)
    ProblemDetail datosInvalidos(DatosInvalidosException ex) {
        return datosInvalidos(List.of(new ErrorDeCampo(ex.getCampo(), ex.getMessage())));
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    ProblemDetail servicioNoDisponible(ServicioNoDisponibleException ex) {
        log.warn("Servicio externo no disponible: {}", ex.getMessage(), ex.getCause());
        return problema(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos", ex);
        return problema(HttpStatus.CONFLICT, "La operación choca con datos existentes.");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail noAutenticado(AuthenticationException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "Necesitás iniciar sesión.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accesoDenegado(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "No tenés permiso para hacer esto.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception ex) {
        log.error("Error no manejado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Probá de nuevo más tarde.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorDeCampo> errores = ex.getBindingResult().getAllErrors().stream()
                .map(e -> new ErrorDeCampo(
                        e instanceof FieldError fe ? fe.getField() : e.getObjectName(),
                        e instanceof FieldError fe && fe.isBindingFailure() ? "Valor inválido." : e.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(datosInvalidos(errores));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorDeCampo> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new ErrorDeCampo(r.getMethodParameter().getParameterName(),
                                e.getDefaultMessage())))
                .toList();
        return ResponseEntity.badRequest().body(datosInvalidos(errores));
    }

    /**
     * Traduce al español el título y el detalle de las excepciones propias de Spring MVC.
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> respuesta = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (respuesta != null && respuesta.getBody() instanceof ProblemDetail problema) {
            problema.setTitle(titulo(statusCode));
            String detalle = detalleEnEspanol(ex);
            if (detalle != null) {
                problema.setDetail(detalle);
            }
        }
        return respuesta;
    }

    private static String detalleEnEspanol(Exception ex) {
        return switch (ex) {
            case NoResourceFoundException e -> "No existe el recurso pedido.";
            case NoHandlerFoundException e -> "No existe el recurso pedido.";
            case HttpRequestMethodNotSupportedException e -> "El método " + e.getMethod() + " no está permitido acá.";
            case HttpMessageNotReadableException e -> "El cuerpo de la solicitud no es un JSON válido.";
            case MissingServletRequestParameterException e -> "Falta el parámetro '" + e.getParameterName() + "'.";
            case TypeMismatchException e -> "El parámetro '" + e.getPropertyName() + "' tiene un valor inválido.";
            default -> null;
        };
    }

    private static ProblemDetail datosInvalidos(List<ErrorDeCampo> errores) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud.");
        problema.setProperty("errores", errores);
        return problema;
    }

    private static ProblemDetail problema(HttpStatus status, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo(status));
        return problema;
    }

    private static String titulo(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Solicitud inválida";
            case 401 -> "No autenticado";
            case 403 -> "Acceso denegado";
            case 404 -> "No encontrado";
            case 405 -> "Método no permitido";
            case 406 -> "Formato no aceptable";
            case 409 -> "Conflicto";
            case 413 -> "Contenido demasiado grande";
            case 415 -> "Tipo de contenido no soportado";
            case 500 -> "Error interno";
            case 503 -> "Servicio no disponible";
            default -> HttpStatus.resolve(status.value()) != null
                    ? HttpStatus.resolve(status.value()).getReasonPhrase()
                    : "Error";
        };
    }
}
