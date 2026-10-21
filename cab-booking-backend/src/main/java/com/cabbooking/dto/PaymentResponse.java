package com.cabbooking.dto;

import com.cabbooking.entity.Payment;
import com.cabbooking.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(Long rideId, BigDecimal amount, String currency, PaymentStatus status, String stripePaymentIntentId, LocalDateTime paidAt) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getRide().getId(), p.getAmount(), p.getCurrency(),
                p.getStatus(), p.getStripePaymentIntentId(), p.getPaidAt());
    }
}
