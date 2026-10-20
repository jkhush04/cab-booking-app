package com.cabbooking.dto;

import com.cabbooking.entity.Driver;

public record DriverRankingResponse(Long driverId, String name, String vehicleModel, double averageRating) {
    public static DriverRankingResponse from(Driver d) {
        return new DriverRankingResponse(d.getId(), d.getUser().getName(), d.getVehicleModel(), d.getAverageRating());
    }
}
