package com.cabbooking.service;

import com.cabbooking.dto.*;
import com.cabbooking.entity.*;
import com.cabbooking.exception.ResourceNotFoundException;
import com.cabbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public RatingResponse rateRide(String riderEmail, Long rideId, RatingRequest req) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));

        if (!ride.getRider().getEmail().equals(riderEmail)) {
            throw new ResourceNotFoundException("Ride not found");
        }
        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new IllegalArgumentException("You can rate only completed rides");
        }
        if (ratingRepository.existsByRideId(rideId)) {
            throw new IllegalArgumentException("Ride already rated");
        }

        Rating rating = ratingRepository.save(Rating.builder()
                .ride(ride)
                .driver(ride.getDriver())
                .stars(req.stars())
                .comment(req.comment())
                .build());

        Double avg = ratingRepository.averageForDriver(ride.getDriver().getId());
        ride.getDriver().setAverageRating(Math.round(avg * 100.0) / 100.0);

        return RatingResponse.from(rating);
    }

    @Transactional(readOnly = true)
    public List<DriverRankingResponse> topDrivers() {
        return driverRepository.findTop10ByAverageRatingGreaterThanOrderByAverageRatingDesc(0.0)
                .stream()
                .map(DriverRankingResponse::from)
                .toList();
    }
}
