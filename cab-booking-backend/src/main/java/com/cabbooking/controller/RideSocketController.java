package com.cabbooking.controller;

import com.cabbooking.dto.*;
import com.cabbooking.service.DriverService;
import com.cabbooking.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class RideSocketController {

    private final DriverService driverService;
    private final RideService rideService;

    @MessageMapping("/ride/{rideId}/location")
    @SendTo("/topic/ride/{rideId}/location")
    public LocationUpdate location(@DestinationVariable Long rideId,
                                   @Payload LocationRequest request,
                                   Principal principal) {
        return driverService.updateRideLocation(principal.getName(), rideId, request);
    }

    @MessageMapping("/ride/{rideId}/arrived")
    @SendTo("/topic/ride/{rideId}/status")
    public RideEvent arrived(@DestinationVariable Long rideId, Principal principal) {
        return driverService.arrived(principal.getName(), rideId);
    }

    @MessageMapping("/ride/{rideId}/sos")
    @SendTo("/topic/ride/{rideId}/sos")
    public RideEvent sos(@DestinationVariable Long rideId, Principal principal) {
        return rideService.sos(principal.getName(), rideId);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleError(Exception ex) {
        return ex.getMessage();
    }
}
