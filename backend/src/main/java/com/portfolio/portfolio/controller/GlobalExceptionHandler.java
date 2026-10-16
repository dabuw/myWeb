package com.portfolio.portfolio.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "message", ex.getMessage()
                ));
    }

    /**
     * 让 ResponseStatusException 的 reason 传到前端。
     *
     * <p>Spring 默认不返回 message（{@code server.error.include-message=never}），
     * 那样限流的提示「请在 X 分钟后重试」就会丢掉，前端只能显示一个 429 状态码。
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();
        String message = ex.getReason();
        if (message == null || message.isBlank()) {
            HttpStatus resolved = HttpStatus.resolve(statusCode.value());
            message = resolved == null ? "Request failed" : resolved.getReasonPhrase();
        }
        return ResponseEntity.status(statusCode)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", statusCode.value(),
                        "message", message
                ));
    }
}
