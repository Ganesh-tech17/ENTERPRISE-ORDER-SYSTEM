package com.example.shop.order.api;

import com.example.shop.order.IdempotencyConflictException;
import com.example.shop.order.client.UserNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
class ApiExceptionHandler {

    record ApiError(String error, String message) {}

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ApiError> userNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ApiError("USER_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ApiError> idempotencyConflict(IdempotencyConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("IDEMPOTENCY_KEY_REUSED", e.getMessage()));
    }

    /** User service down / circuit open: fail fast with 503 so clients can retry with the SAME Idempotency-Key. */
    @ExceptionHandler({CallNotPermittedException.class, ResourceAccessException.class, HttpServerErrorException.class})
    ResponseEntity<ApiError> downstreamUnavailable(Exception e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "10")
                .body(new ApiError("DEPENDENCY_UNAVAILABLE", "User service is unavailable, please retry shortly"));
    }
}
