package com.cabbooking.repository;

import com.cabbooking.entity.Ride;
import com.cabbooking.entity.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByRiderId(Long riderId);
    List<Ride> findByDriverId(Long driverId);
    List<Ride> findByStatus(RideStatus status);
}