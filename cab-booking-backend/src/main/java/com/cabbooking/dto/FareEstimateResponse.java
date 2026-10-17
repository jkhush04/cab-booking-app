package com.cabbooking.dto;

import java.math.BigDecimal;

public record FareEstimateResponse(double distanceKm, BigDecimal fare){
}
