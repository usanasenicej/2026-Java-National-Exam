package com.wasac.ne.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");

    private String fullNames;
    private String password;
    private String phoneNumber;

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return false;
        }

        if (!email.equals(email.toLowerCase())) {
            setMessage(context, "Email must be in lowercase letters only");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            setMessage(context, "Email must be a valid format (e.g. user@domain.com)");
            return false;
        }

        String localPart = email.split("@")[0];
        if (fullNames != null && !fullNames.isBlank()
                && localPart.equalsIgnoreCase(fullNames.replaceAll("\\s+", "").toLowerCase())) {
            setMessage(context, "Email cannot be derived from your full name");
            return false;
        }
        if (password != null && !password.isBlank() && localPart.equalsIgnoreCase(password.toLowerCase())) {
            setMessage(context, "Email cannot match or be derived from your password");
            return false;
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            String normalizedPhone = phoneNumber.replaceAll("[^0-9]", "");
            if (localPart.equals(normalizedPhone) || email.contains(normalizedPhone)) {
                setMessage(context, "Email cannot contain or match your phone number");
                return false;
            }
        }

        return true;
    }

    public void setCrossFieldValues(String fullNames, String password, String phoneNumber) {
        this.fullNames = fullNames;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
