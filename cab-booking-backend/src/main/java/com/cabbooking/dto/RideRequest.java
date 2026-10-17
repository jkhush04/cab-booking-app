package com.cabbooking.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RideRequest (
    @NotBlank String pickupAddress,
    @NotNull @DecimalMin("-90") @DecimalMax("90") Double pickupLat,
    @NotNull @DecimalMin("-180") @DecimalMax("180") Double pickupLng,
    @NotBlank String dropAddress,
    @NotNull @DecimalMin("-90") @DecimalMax("90") Double dropLat,
    @NotNull @DecimalMin("-180") @DecimalMax("180") Double dropLng
) {}
