package com.pharmacy.demo.util;

public final class AccountLinks {

    private static final String ACTIVATION = "/pages/auth/activate.html?token=";
    private static final String RESET = "/pages/auth/reset-password.html?token=";

    private AccountLinks() {}

    public static String activation(String token) {
        return ACTIVATION + token;
    }

    public static String reset(String token) {
        return RESET + token;
    }
}
