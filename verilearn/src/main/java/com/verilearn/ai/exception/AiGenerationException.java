package com.verilearn.ai.exception;

public class AiGenerationException extends IllegalStateException {

    public AiGenerationException(String message) {
        super(message);
    }

    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
