package com.example.leave.validation;

import com.example.leave.dto.LeaveApplyRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, LeaveApplyRequest> {

    @Override
    public boolean isValid(LeaveApplyRequest request, ConstraintValidatorContext context) {
        if (request == null || request.startDate() == null || request.endDate() == null) {
            return true; // @NotNull on the fields reports missing values
        }
        if (request.endDate().isBefore(request.startDate())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("endDate must be on or after startDate")
                    .addPropertyNode("endDate")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
