package com.pharmacy.demo.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class UnprocessableException extends BusinessException {

    public UnprocessableException(String code, String message, Map<String, String> fieldErrors) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message, fieldErrors);
    }
}
