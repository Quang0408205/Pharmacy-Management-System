package com.pharmacy.demo.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }

    public ForbiddenException(String message) {
        this("FORBIDDEN", message);
    }

    public ForbiddenException() {
        this("Bạn không có quyền thực hiện thao tác này.");
    }
}
