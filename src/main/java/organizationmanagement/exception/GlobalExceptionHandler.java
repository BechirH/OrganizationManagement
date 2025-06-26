package organizationmanagement.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, "Not Found");
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Bad Request");
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Object> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Service unavailable: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable");
    }

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<Object> handleFeignNotFound(FeignException.NotFound ex) {
        log.warn("Feign client - Resource not found: {}", ex.getMessage());
        return buildErrorResponse(
                new ResourceNotFoundException("Requested resource not found in downstream service"),
                HttpStatus.NOT_FOUND,
                "Not Found"
        );
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> handleFeignException(FeignException ex) {
        log.error("Feign client error: {}", ex.getMessage());
        return buildErrorResponse(
                new ServiceUnavailableException("Downstream service unavailable: " + ex.getMessage()),
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(
                ex,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error"
        );
    }

    private ResponseEntity<Object> buildErrorResponse(Exception ex, HttpStatus status, String error) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", ex.getMessage());

        // Include the exception class for debugging in development
        body.put("exception", ex.getClass().getSimpleName());

        return new ResponseEntity<>(body, status);
    }
}