package com.example.leave.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** Class-level constraint: endDate must not be before startDate. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface ValidDateRange {

    String message() default "endDate must be on or after startDate";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
