package com.cabbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rider_id")
    private User rider;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    private String pickupAddress;
    private double pickupLat;
    private double pickupLng;

    private String dropAddress;
    private double dropLat;
    private double dropLng;

    private double distanceKm;
    private BigDecimal fare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;

    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();
}