package com.cabbooking.service;

import com.cabbooking.dto.FareEstimateResponse;
import com.cabbooking.dto.RideRequest;
import com.cabbooking.dto.RideResponse;
import com.cabbooking.entity.Ride;
import com.cabbooking.entity.RideStatus;
import com.cabbooking.entity.User;
import com.cabbooking.exception.ResourceNotFoundException;
import com.cabbooking.repository.RideRepository;
import com.cabbooking.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RideService {


        private final RideRepository rideRepository;
        private final FareService fareService;
        private final UserRepository userRepository;


        public FareEstimateResponse estimate(RideRequest req){
            double km=fareService.distanceKm(req.pickupLat(), req.pickupLng(),req.dropLat(),req.dropLng());
            return new FareEstimateResponse(km, fareService.calculateFare(km));
        }

        @Transactional
        public RideResponse bookRide(String riderEmail, RideRequest req){
            User rider=userRepository.findByEmail(riderEmail)
                    .orElseThrow(()->new ResourceNotFoundException("User with email "+riderEmail+" not found"));
            double km=fareService.distanceKm(req.pickupLat(), req.pickupLng(),req.dropLat(),req.dropLng());

            Ride ride=rideRepository.save(Ride.builder()
                    .rider(rider)
                    .pickupAddress(req.pickupAddress())
                    .pickupLat(req.pickupLat())
                    .pickupLng(req.pickupLng())
                    .dropAddress(req.dropAddress())
                    .dropLat(req.dropLat())
                    .dropLng(req.dropLng())
                    .distanceKm(km)
                    .fare(fareService.calculateFare(km))
                    .status(RideStatus.REQUESTED)
                    .build()
            );

            return RideResponse.from(ride);


        }

        @Transactional
        public List<RideResponse> myRides(String riderEmail){
            User rider=userRepository.findByEmail(riderEmail)
                    .orElseThrow(()->new ResourceNotFoundException("User with email "+riderEmail+" not found"));
            return rideRepository.findByRiderId(rider.getId()).stream()
                    .map(RideResponse::from)
                    .toList();
        }

        @Transactional
        public RideResponse getRide(Long id, String email){
              Ride ride=rideRepository.findById(id)
                    .orElseThrow(()->new ResourceNotFoundException("Ride with id "+id+" not found"));

              boolean isRider = ride.getRider().getEmail().equals(email);
              boolean isDriver=ride.getDriver()!=null && ride.getDriver().getUser().getEmail().equals(email);
              if(!isDriver && !isRider){
                  throw new ResourceNotFoundException("User with email "+email+" is not authorized to access this ride");

              }
              return RideResponse.from(ride);
        }
}
