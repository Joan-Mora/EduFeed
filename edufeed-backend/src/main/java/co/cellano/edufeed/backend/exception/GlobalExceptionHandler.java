package co.cellano.edufeed.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Maneja todas las excepciones de la API y devuelve respuestas estandarizadas
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private void setLoggingContext(HttpServletRequest request) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
        MDC.put("username", username);

        MDC.put("path", request.getRequestURI());
        MDC.put("method", request.getMethod());
    }

    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception ex, HttpServletRequest request) {
        setLoggingContext(request);
        logger.warn("Acceso denegado: {}", ex.getMessage());
        clearLoggingContext();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Acceso denegado",
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler({ AuthenticationCredentialsNotFoundException.class, AuthenticationException.class })
    public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception ex, HttpServletRequest request) {
        setLoggingContext(request);
        logger.warn("No autenticado: {}", ex.getMessage());
        clearLoggingContext();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "No autenticado o credenciales inválidas",
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    private void clearLoggingContext() {
        MDC.clear();
    }

    @ExceptionHandler(DuplicateDocumentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateDocument(DuplicateDocumentException ex,
            HttpServletRequest request) {
        setLoggingContext(request);
        logger.warn("Documento duplicado: {}", ex.getMessage());
        clearLoggingContext();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "DUPLICATE_DOCUMENT",
                ex.getMessage(),
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
            HttpServletRequest request) {
        setLoggingContext(request);
        logger.warn("Recurso no encontrado: {} - ID: {}", ex.getResourceType(), ex.getIdentifier());
        clearLoggingContext();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidBusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBusinessRule(InvalidBusinessRuleException ex,
            HttpServletRequest request) {
        setLoggingContext(request);
        logger.warn("Regla de negocio violada: {}", ex.getMessage());
        clearLoggingContext();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_BUSINESS_RULE",
                ex.getMessage(),
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(BiometricEnrollmentException.class)
    public ResponseEntity<ErrorResponse> handleBiometricEnrollment(BiometricEnrollmentException ex,
            HttpServletRequest request) {
        setLoggingContext(request);
        logger.error("Error en registro biométrico: {}", ex.getMessage(), ex);
        clearLoggingContext();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "BIOMETRIC_ENROLLMENT_FAILED",
                ex.getMessage(),
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(BiometricVerificationException.class)
    public ResponseEntity<ErrorResponse> handleBiometricVerification(BiometricVerificationException ex,
            HttpServletRequest request) {
        setLoggingContext(request);
        logger.error("Error en verificación biométrica: {}", ex.getMessage(), ex);
        clearLoggingContext();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "BIOMETRIC_VERIFICATION_FAILED",
                ex.getMessage(),
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPayment(InvalidPaymentException ex, HttpServletRequest request) {
        setLoggingContext(request);
        logger.warn("Pago inválido: {}", ex.getMessage());
        clearLoggingContext();

        String errorCode = ex.getMessage().contains("YA_APROBADO") ? "PAGO_YA_APROBADO"
                : ex.getMessage().contains("YA_RECHAZADO") ? "PAGO_YA_RECHAZADO"
                        : ex.getMessage().contains("PREVIAMENTE_RECHAZADO") ? "PAGO_PREVIAMENTE_RECHAZADO"
                                : "INVALID_PAYMENT";
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorCode,
                ex.getMessage(),
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InvalidVigenciaException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVigencia(InvalidVigenciaException ex,
            HttpServletRequest request) {
        setLoggingContext(request);
        logger.warn("Vigencia inválida: {}", ex.getMessage());
        clearLoggingContext();

        String errorCode = ex.getMessage().contains("INCOHERENTES") ? "VIGENCIAS_INCOHERENTES"
                : ex.getMessage().contains("FALTANTES") ? "VIGENCIAS_FALTANTES"
                        : "INVALID_VIGENCIA";
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorCode,
                ex.getMessage(),
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InsufficientPackageException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPackage(InsufficientPackageException ex,
            HttpServletRequest request) {
        setLoggingContext(request);
        logger.warn("Días de paquete insuficientes: {}", ex.getMessage());
        clearLoggingContext();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INSUFFICIENT_PACKAGE_DAYS",
                ex.getMessage(),
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NoDerechoVigenteException.class)
    public ResponseEntity<ErrorResponse> handleNoDerechoVigente(NoDerechoVigenteException ex,
            HttpServletRequest request) {
        setLoggingContext(request);
        logger.warn("Sin derecho vigente: {}", ex.getMessage());
        clearLoggingContext();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "NO_VALID_ACCESS_RIGHT",
                ex.getMessage(),
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        setLoggingContext(request);

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        logger.warn("Errores de validación: {}", errors);
        clearLoggingContext();

        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Error de validación en los campos de entrada",
                OffsetDateTime.now(),
                errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        setLoggingContext(request);
        logger.error("Error inesperado: {}", ex.getMessage(), ex);
        clearLoggingContext();

        // No exponemos detalles del error excepto para OpenAPI
        String path = request.getRequestURI();
        boolean isOpenApi = path != null && (path.startsWith("/api-docs") || path.startsWith("/v3/api-docs"));
        String message = isOpenApi
                ? (ex.getMessage() != null ? ex.getMessage() : "OpenAPI error")
                : "Ha ocurrido un error interno. Por favor, contacte al administrador.";

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                message,
                OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    public static class ErrorResponse {
        private int status;
        private String code;
        private String message;
        private OffsetDateTime timestamp;

        public ErrorResponse(int status, String code, String message, OffsetDateTime timestamp) {
            this.status = status;
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class ValidationErrorResponse extends ErrorResponse {
        private Map<String, String> fieldErrors;

        public ValidationErrorResponse(int status, String code, String message, OffsetDateTime timestamp,
                Map<String, String> fieldErrors) {
            super(status, code, message, timestamp);
            this.fieldErrors = fieldErrors;
        }

        public Map<String, String> getFieldErrors() {
            return fieldErrors;
        }

        public void setFieldErrors(Map<String, String> fieldErrors) {
            this.fieldErrors = fieldErrors;
        }
    }
}
