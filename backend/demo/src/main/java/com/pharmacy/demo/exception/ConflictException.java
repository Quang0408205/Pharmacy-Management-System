package com.pharmacy.demo.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {

    public ConflictException(String message, Map<String, String> fieldErrors) {
        super(HttpStatus.CONFLICT, "CONFLICT", message, fieldErrors);
    }

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
