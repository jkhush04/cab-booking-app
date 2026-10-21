package com.cabbooking.dto;

import java.math.BigDecimal;

public record PaymentIntentResponse(Long paymenId, String paymnetIntentIdn, String clientSecret, BigDecimal amount , String currency) {
}
