package com.cabbooking.dto;

import com.cabbooking.entity.RideStatus;
import java.time.LocalDateTime;

public record RideEvent(String type, Long rideId, RideStatus status, String message, LocalDateTime timestamp) {}