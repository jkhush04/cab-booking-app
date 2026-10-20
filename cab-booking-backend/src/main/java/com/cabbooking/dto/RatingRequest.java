package com.cabbooking.dto;


import jakarta.validation.constraints.*;

public record RatingRequest(
        @NotNull @Min(1) @Max(5) int stars,
        @Size(max=500) String comment
) {
}
