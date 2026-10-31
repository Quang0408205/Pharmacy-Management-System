package com.pharmacy.demo.util;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class PhoneMatcher {

    public static final String PATTERN = "^(0|\\+84)[\\d\\s]{8,12}$";
    public static final String MESSAGE = "Số điện thoại không hợp lệ (ví dụ 0901 234 567).";

    private PhoneMatcher() {}

    public static String sameNumberRegex(String phone) {
        String compact = phone.replaceAll("\\s+", "");
        String body = compact.chars()
                .mapToObj(character -> Pattern.quote(String.valueOf((char) character)))
                .collect(Collectors.joining("\\s*"));
        return "^\\s*" + body + "\\s*$";
    }
}
