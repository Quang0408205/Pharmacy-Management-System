package com.pharmacy.demo.util;

public final class PasswordPolicy {

    public static final String PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
    public static final String MESSAGE = "Mật khẩu tối thiểu 8 ký tự, gồm chữ hoa, chữ thường và số.";

    private PasswordPolicy() {}
}
