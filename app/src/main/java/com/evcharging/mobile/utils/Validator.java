package com.evcharging.mobile.utils;

import java.util.regex.Pattern;

public class Validator {

    private static final String EMAIL_PATTERN =
            "^[A-Za-z0-9+_.-]+@(.+)$";

    private static final String NIC_PATTERN =
            "^[0-9]{9}[vVxX]?$|^[0-9]{12}$";

    private static final String PHONE_PATTERN =
            "^[0-9]{10}$";

    public static boolean isValidEmail(String email) {
        return Pattern.compile(EMAIL_PATTERN).matcher(email).matches();
    }

    public static boolean isValidNIC(String nic) {
        return Pattern.compile(NIC_PATTERN).matcher(nic).matches();
    }

    public static boolean isValidPhone(String phone) {
        return Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isFutureDate(java.util.Date date) {
        return date != null && date.after(new java.util.Date());
    }
}