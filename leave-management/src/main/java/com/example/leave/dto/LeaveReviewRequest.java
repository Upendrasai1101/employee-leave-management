package com.example.leave.dto;

import jakarta.validation.constraints.Size;

/** Optional body for approve/reject. */
public record LeaveReviewRequest(
        @Size(max = 500, message = "comment must be at most 500 characters")
        String comment
) {
}
