package com.pharmacy.demo.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class BadRequestException extends BusinessException {

    public BadRequestException(String message, Map<String, String> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, fieldErrors);
    }

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
