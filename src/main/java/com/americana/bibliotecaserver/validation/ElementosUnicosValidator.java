package com.americana.bibliotecaserver.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.List;

public class ElementosUnicosValidator implements ConstraintValidator<ElementosUnicos, List<?>> {
    @Override
    public boolean isValid(List<?> valores, ConstraintValidatorContext context) {
        return valores == null || new HashSet<>(valores).size() == valores.size();
    }
}
