package com.wasac.ne.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {

    String message() default "Invalid email format. Email must be lowercase, valid format, and cannot match name, password, or phone number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
