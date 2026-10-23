package com.cabbooking.dto;

import com.cabbooking.entity.Ride;
import com.cabbooking.entity.RideStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RideResponse (
        Long id,
        RideStatus status,
        String pickupAddress,
        String dropAddress,
        double distanceKm,
        BigDecimal fare,
        String driverName,
        LocalDateTime requestedAt,
        double pickupLat,
        double pickupLng,
        double dropLat,
        double dropLng
){

    public static RideResponse from(Ride r){
        return new RideResponse(
                r.getId(),
                r.getStatus(),
                r.getPickupAddress(),
                r.getDropAddress(),
                r.getDistanceKm(),
                r.getFare(),
                r.getDriver() != null ? r.getDriver().getUser().getName() : null,
                r.getRequestedAt(),
                r.getPickupLat(),
                r.getPickupLng(),
                r.getDropLat(),
                r.getDropLng()
        );
    }
}
