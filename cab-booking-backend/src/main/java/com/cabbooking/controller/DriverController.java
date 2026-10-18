package com.cabbooking.controller;

import com.cabbooking.dto.*;
import com.cabbooking.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
@PreAuthorize("hasRole('DRIVER')")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PutMapping("/status")
    public MessageResponse status(@RequestParam boolean available, Authentication auth) {
        return driverService.setAvailability(auth.getName(), available);
    }

    @PutMapping("/location")
    public MessageResponse location(@Valid @RequestBody LocationRequest request, Authentication auth) {
        return driverService.updateLocation(auth.getName(), request);
    }

    @GetMapping("/requests")
    public List<RideResponse> requests(Authentication auth) {
        return driverService.nearbyRequests(auth.getName());
    }

    @PostMapping("/rides/{id}/accept")
    public RideResponse accept(@PathVariable Long id, Authentication auth) {
        return driverService.accept(auth.getName(), id);
    }

    @PostMapping("/rides/{id}/decline")
    public MessageResponse decline(@PathVariable Long id, Authentication auth) {
        return driverService.decline(auth.getName(), id);
    }

    @PostMapping("/rides/{id}/start")
    public RideResponse start(@PathVariable Long id, Authentication auth) {
        return driverService.start(auth.getName(), id);
    }

    @PostMapping("/rides/{id}/complete")
    public RideResponse complete(@PathVariable Long id, Authentication auth) {
        return driverService.complete(auth.getName(), id);
    }
}