package com.cabbooking.dto;

import java.math.BigDecimal;

public record EarningsResponse(BigDecimal totalEarnings, long completedRides, double averageRating) {
}
