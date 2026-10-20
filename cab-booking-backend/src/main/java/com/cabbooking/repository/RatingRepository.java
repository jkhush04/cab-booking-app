package com.cabbooking.repository;

import com.cabbooking.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    boolean existsByRideId(Long rideId);
    List<Rating> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    @Query("select avg(r.stars) from Rating r where r.driver.id = :driverId")
    Double averageForDriver(@Param("driverId") Long driverId);
}
