package com.cabbooking.service;

import com.cabbooking.dto.PaymentIntentResponse;
import com.cabbooking.dto.PaymentResponse;
import com.cabbooking.entity.*;
import com.cabbooking.exception.ResourceNotFoundException;
import com.cabbooking.repository.PaymentRepository;
import com.cabbooking.repository.RideRepository;
import com.cabbooking.websocket.RideNotifier;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String CURRENCY = "inr";

    private final PaymentRepository paymentRepository;
    private final RideRepository rideRepository;
    private final RideNotifier notifier;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public PaymentIntentResponse createIntent(String riderEmail, Long rideId) throws StripeException {
        Ride ride = findRiderRide(riderEmail, rideId);

        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new IllegalArgumentException("You can pay only after the ride is completed");
        }

        Optional<Payment> existing = paymentRepository.findByRideId(rideId);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                throw new IllegalArgumentException("Ride already paid");
            }
            PaymentIntent intent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
            if ("succeeded".equals(intent.getStatus())) {
                markSucceeded(intent.getId());
                throw new IllegalArgumentException("Ride already paid");
            }
            return toResponse(payment, intent);
        }

        long amountInPaise = ride.getFare()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInPaise)
                .setCurrency(CURRENCY)
                .putMetadata("rideId", String.valueOf(rideId))
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(
                                        PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                .build())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        Payment payment = paymentRepository.save(Payment.builder()
                .ride(ride)
                .amount(ride.getFare())
                .currency(CURRENCY)
                .stripePaymentIntentId(intent.getId())
                .status(PaymentStatus.PENDING)
                .build());

        return toResponse(payment, intent);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String riderEmail, Long rideId) {
        findRiderRide(riderEmail, rideId);
        Payment payment = paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment for this ride"));
        return PaymentResponse.from(payment);
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        String secret = webhookSecret.trim();
        log.info("Webhook secret length={}, ends with {}", secret.length(),
                secret.substring(Math.max(0, secret.length() - 4)));

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, secret);
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> markSucceeded(extractIntentId(event));
            case "payment_intent.payment_failed" -> markFailed(extractIntentId(event));
            default -> { }
        }
    }

    private void markSucceeded(String intentId) {
        paymentRepository.findByStripePaymentIntentId(intentId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                return;
            }
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setPaidAt(LocalDateTime.now());
            notifier.paymentSucceeded(payment.getRide());
        });
    }

    private void markFailed(String intentId) {
        paymentRepository.findByStripePaymentIntentId(intentId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
            }
        });
    }

    private String extractIntentId(Event event) {
        try {
            String raw = event.getDataObjectDeserializer().getRawJson();
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw).get("id").asText();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot read webhook event");
        }
    }

    private Ride findRiderRide(String riderEmail, Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
        if (!ride.getRider().getEmail().equals(riderEmail)) {
            throw new ResourceNotFoundException("Ride not found");
        }
        return ride;
    }

    private PaymentIntentResponse toResponse(Payment payment, PaymentIntent intent) {
        return new PaymentIntentResponse(payment.getId(), intent.getId(), intent.getClientSecret(),
                payment.getAmount(), payment.getCurrency());
    }
}