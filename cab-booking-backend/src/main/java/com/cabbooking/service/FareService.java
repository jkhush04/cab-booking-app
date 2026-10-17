package com.cabbooking.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareService {


    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double BASE_FARE = 40.0;
    private static final double PER_KM = 12.0;
    private static final double MIN_FARE = 60.0;


    public double distanceKm(double lat1, double lng1, double lat2, double lng2){
        double dLat=Math.toRadians(lat2-lat1);
        double dLng=Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2 )* Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng/2)*Math.sin(dLng/2);

        double km=2* EARTH_RADIUS_KM * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return Math.round(km*100.0)/100.0;
    }

    public BigDecimal calculateFare(double distanceKm){
        double fare = Math.max(BASE_FARE + PER_KM * distanceKm, MIN_FARE);
        return BigDecimal.valueOf(fare).setScale(2, RoundingMode.HALF_UP);
    }
}
