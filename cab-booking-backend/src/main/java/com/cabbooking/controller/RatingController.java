package com.cabbooking.controller;

import com.cabbooking.dto.*;
import com.cabbooking.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/api/rides/{id}/rate")
    @PreAuthorize("hasRole('RIDER')")
    public RatingResponse rate(@PathVariable Long id,
                               @Valid @RequestBody RatingRequest request,
                               Authentication auth) {
        return ratingService.rateRide(auth.getName(), id, request);
    }

    @GetMapping("/api/drivers/top")
    public List<DriverRankingResponse> topDrivers() {
        return ratingService.topDrivers();
    }
}
