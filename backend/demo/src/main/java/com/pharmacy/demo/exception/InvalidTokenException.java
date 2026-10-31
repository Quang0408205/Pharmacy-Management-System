package com.pharmacy.demo.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BusinessException {

    public InvalidTokenException() {
        super(HttpStatus.CONFLICT, "INVALID_TOKEN", "Liên kết không hợp lệ hoặc đã hết hạn.");
    }
}
