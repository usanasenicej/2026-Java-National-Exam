package com.wasac.ne.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!*()_\\-]).{8,}$");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            setMessage(context, "Password must not be empty");
            return false;
        }
        if (password.length() < 8) {
            setMessage(context, "Password must be at least 8 characters long");
            return false;
        }
        if (!STRONG_PASSWORD.matcher(password).matches()) {
            setMessage(context, "Password must contain uppercase, lowercase, digit, and special character (@#$%^&+=!*()_-)");
            return false;
        }
        return true;
    }

    private void setMessage(ConstraintValidatorContext context, String message) {
        if (context == null) {
            return;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
