package com.verilearn.common;

import com.verilearn.ai.exception.AiGenerationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ApiResponse.error(400, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().isEmpty()
                ? "invalid request"
                : exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(AiGenerationException.class)
    public ApiResponse<Void> handleAiGenerationException(AiGenerationException exception) {
        return ApiResponse.error(502, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.error(500, exception.getMessage());
    }
}
