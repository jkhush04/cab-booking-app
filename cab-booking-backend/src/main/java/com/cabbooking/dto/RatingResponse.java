package com.cabbooking.dto;

import com.cabbooking.entity.Rating;

import java.time.LocalDateTime;

public record RatingResponse(Long rideId, int stars, String comment, LocalDateTime createdAt) {
    public static RatingResponse from(Rating r) {
        return new RatingResponse(r.getRide().getId(), r.getStars(), r.getComment(), r.getCreatedAt());
    }
}
