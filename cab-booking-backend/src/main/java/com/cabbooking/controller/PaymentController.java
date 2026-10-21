package com.cabbooking.controller;

import com.cabbooking.dto.PaymentIntentResponse;
import com.cabbooking.dto.PaymentResponse;
import com.cabbooking.service.PaymentService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/ride/{rideId}/intent")
    @PreAuthorize("hasRole('RIDER')")
    public PaymentIntentResponse createIntent(@PathVariable Long rideId, Authentication auth)
            throws StripeException {
        return paymentService.createIntent(auth.getName(), rideId);
    }

    @GetMapping("/ride/{rideId}")
    @PreAuthorize("hasRole('RIDER')")
    public PaymentResponse getPayment(@PathVariable Long rideId, Authentication auth) {
        return paymentService.getPayment(auth.getName(), rideId);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader("Stripe-Signature") String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok("ok");
    }
}