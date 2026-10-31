package com.pharmacy.demo.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }

    public UnauthorizedException(String message) {
        this("UNAUTHORIZED", message);
    }

    public UnauthorizedException() {
        this("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    }
}
