package com.cabbooking.service;

import com.cabbooking.dto.*;
import com.cabbooking.entity.Driver;
import com.cabbooking.entity.Ride;
import com.cabbooking.entity.RideStatus;
import com.cabbooking.exception.ResourceNotFoundException;
import com.cabbooking.repository.DriverRepository;
import com.cabbooking.repository.RatingRepository;
import com.cabbooking.repository.RideRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.cabbooking.websocket.RideNotifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private static final double NEARBY_KM=5.0;
    private static final List<RideStatus> ACTIVE=List.of(RideStatus.ACCEPTED, RideStatus.STARTED);

    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final FareService fareService;
    private final RideNotifier notifier;
    private final RatingRepository ratingRepository;

    private Driver getDriver(String email){
        return driverRepository.findByUserEmail(email)
                .orElseThrow(()->new ResourceNotFoundException("Driver not found"));
    }


    private Ride getOwnRide(Long rideId, Driver driver) {
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));

        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driver.getId())) {
            throw new ResourceNotFoundException("Ride not found");
        }
     return ride;
    }

    @Transactional
    public MessageResponse setAvailability(String email, boolean available){
        Driver driver=getDriver(email);
        if(available && rideRepository.existsByDriverIdAndStatusIn(driver.getId(),ACTIVE)){
            throw new IllegalArgumentException("Finish your current ride first");
        }
        driver.setAvailable(available);
        return new MessageResponse(available?"You are online":"You are offline");
    }

    @Transactional
    public MessageResponse updateLocation(String email, LocationRequest req){
        Driver driver=getDriver(email);
        driver.setCurrentLatitude(req.lat());
        driver.setCurrentLongitude(req.lng());
        return new MessageResponse("Location updated successfully");
    }

    @Transactional(readOnly = true)
    public List<RideResponse> nearbyRequests(String email){
        Driver driver=getDriver(email);
        if(!driver.isAvailable()){
            throw new IllegalArgumentException("Go Online to see ride requests");

        }
        if(driver.getCurrentLatitude()==null || driver.getCurrentLongitude()==null){
            throw new IllegalArgumentException("Update your location to see ride requests");
        }

        return rideRepository.findByStatus(RideStatus.REQUESTED).stream()
                .filter(r->fareService.distanceKm(
                        driver.getCurrentLatitude(),driver.getCurrentLongitude(),
                        r.getPickupLat(),r.getPickupLng())<=NEARBY_KM)
                        .map(RideResponse::from)
                        .toList();
    }

 @Transactional
    public RideResponse accept(String email, Long rideId){
        Driver driver=getDriver(email);
        if(!driver.isAvailable()){
            throw new IllegalArgumentException("go online first");
        }
        Ride ride= rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(()->new ResourceNotFoundException("Ride not found"));
        if(ride.getStatus()!=RideStatus.REQUESTED){
            throw new IllegalArgumentException("Ride is no longer available");
        }
        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);
     notifier.statusChanged(ride, "Driver accepted your ride");
     driver.setAvailable(false);
        return RideResponse.from(ride);
    }

    @Transactional(readOnly = true)
    public MessageResponse decline(String email, Long rideId){
        getDriver(email);
        Ride ride=rideRepository.findById(rideId)
                .orElseThrow(()->new ResourceNotFoundException("Ride not found"));
        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new IllegalArgumentException("Ride is no longer available");
        }
        return new MessageResponse("Ride declined");
    }


    @Transactional
    public RideResponse start(String email, Long rideId) {
        Driver driver = getDriver(email);
        Ride ride = getOwnRide(rideId, driver);
        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new IllegalArgumentException("Ride cannot be started");
        }
        ride.setStatus(RideStatus.STARTED);
        notifier.statusChanged(ride, "Ride started");
        return RideResponse.from(ride);
    }


    @Transactional
    public RideResponse complete(String email, Long rideId) {
        Driver driver = getDriver(email);
        Ride ride = getOwnRide(rideId, driver);
        if (ride.getStatus() != RideStatus.STARTED) {
            throw new IllegalArgumentException("Ride is not in progress");
        }
        ride.setStatus(RideStatus.COMPLETED);
        driver.setAvailable(true);
        notifier.statusChanged(ride, "Ride completed");
        return RideResponse.from(ride);
    }

    @Transactional
    public LocationUpdate updateRideLocation(String email, Long rideId, LocationRequest req) {
        Driver driver = getDriver(email);
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driver.getId())
                || !ACTIVE.contains(ride.getStatus())) {
            throw new IllegalArgumentException("No active ride");
        }
        driver.setCurrentLatitude(req.lat());
        driver.setCurrentLongitude(req.lng());
        return new LocationUpdate(rideId, req.lat(), req.lng());
    }

    @Transactional
    public RideEvent arrived(String email, Long rideId) {
        Driver driver = getDriver(email);
        Ride ride = getOwnRide(rideId, driver);
        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new IllegalArgumentException("Ride is not in accepted state");
        }
        return new RideEvent("DRIVER_ARRIVED", rideId, ride.getStatus(),
                "Your driver has arrived", LocalDateTime.now());
    }


    @Transactional(readOnly = true)
    public List<RideResponse> myRides(String email) {
        Driver driver = getDriver(email);
        return rideRepository.findByDriverIdOrderByRequestedAtDesc(driver.getId()).stream()
                .map(RideResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EarningsResponse earnings(String email) {
        Driver driver = getDriver(email);
        BigDecimal total = rideRepository.totalEarnings(driver.getId());
        long completed = rideRepository.countByDriverIdAndStatus(driver.getId(), RideStatus.COMPLETED);
        return new EarningsResponse(total != null ? total : BigDecimal.ZERO, completed, driver.getAverageRating());
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> myRatings(String email) {
        Driver driver = getDriver(email);
        return ratingRepository.findByDriverIdOrderByCreatedAtDesc(driver.getId()).stream()
                .map(RatingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse profile(String email) {
        return DriverProfileResponse.from(getDriver(email));
    }
    }



