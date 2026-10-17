package com.cabbooking.controller;


import com.cabbooking.dto.FareEstimateResponse;
import com.cabbooking.dto.RideRequest;
import com.cabbooking.dto.RideResponse;
import com.cabbooking.entity.Ride;
import com.cabbooking.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    @PostMapping("/estimate")
    public FareEstimateResponse estimate(@Valid @RequestBody RideRequest request){
        return rideService.estimate(request);
    }

    @PostMapping("/book")
    @PreAuthorize("hasRole('RIDER')")
    public RideResponse book(@Valid @RequestBody RideRequest request, Authentication auth){
         return rideService.bookRide(auth.getName(), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('RIDER')")
    public List<RideResponse> myRides(Authentication auth){
        return rideService.myRides(auth.getName());
    }

    @GetMapping("/{id}")
    public RideResponse getRide(@PathVariable Long id,Authentication auth){
        return rideService.getRide(id, auth.getName());
    }

}
