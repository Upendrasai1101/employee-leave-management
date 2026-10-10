package com.example.leave.validation;

import com.example.leave.dto.LeaveApplyRequest;
import com.example.leave.entity.LeaveType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveApplyRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private Set<String> invalidFields(LeaveApplyRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("A well-formed request has no violations")
    void validRequest_noViolations() {
        LocalDate start = LocalDate.now().plusDays(3);
        var request = new LeaveApplyRequest(LeaveType.SICK, start, start.plusDays(1), "Fever");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("endDate before startDate is flagged by the custom @ValidDateRange validator")
    void endBeforeStart_flagged() {
        LocalDate start = LocalDate.now().plusDays(5);
        var request = new LeaveApplyRequest(LeaveType.SICK, start, start.minusDays(2), "Fever");

        assertThat(invalidFields(request)).contains("endDate");
    }

    @Test
    @DisplayName("Past start date is flagged by @FutureOrPresent")
    void pastStartDate_flagged() {
        LocalDate start = LocalDate.now().minusDays(1);
        var request = new LeaveApplyRequest(LeaveType.CASUAL, start, start.plusDays(2), "Trip");

        assertThat(invalidFields(request)).contains("startDate");
    }

    @Test
    @DisplayName("Missing fields are all reported")
    void missingFields_flagged() {
        var request = new LeaveApplyRequest(null, null, null, " ");

        assertThat(invalidFields(request))
                .contains("leaveType", "startDate", "endDate", "reason");
    }
}
