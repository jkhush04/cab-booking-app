package com.cabbooking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "drivers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String vehicleModel;
    private String vehicleNumber;

    @Builder.Default
    private boolean available = false;

    private Double currentLatitude;
    private Double currentLongitude;

    @Builder.Default
    private double averageRating = 0.0;
}