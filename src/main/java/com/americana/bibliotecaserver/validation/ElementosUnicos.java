package com.americana.bibliotecaserver.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ElementosUnicosValidator.class)
public @interface ElementosUnicos {
    String message() default "No puede contener IDs repetidos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
