package com.pharmacy.demo.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public NotFoundException() {
        this("Không tìm thấy dữ liệu yêu cầu.");
    }
}
