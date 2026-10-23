package com.cabbooking.dto;

import com.cabbooking.entity.Driver;

public record DriverProfileResponse(String name, String vehicleModel, String vehicleNumber,
                                    boolean available, Double lat, Double lng, double averageRating) {

    public static DriverProfileResponse from(Driver d) {
        return new DriverProfileResponse(d.getUser().getName(), d.getVehicleModel(), d.getVehicleNumber(),
                d.isAvailable(), d.getCurrentLatitude(), d.getCurrentLongitude(), d.getAverageRating());
    }
}
