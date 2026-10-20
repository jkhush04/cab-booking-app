package com.cabbooking.websocket;

import com.cabbooking.dto.RideEvent;
import com.cabbooking.dto.RideResponse;
import com.cabbooking.entity.Ride;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RideNotifier {

    private final SimpMessagingTemplate template;

    public void statusChanged(Ride ride, String message) {
        RideEvent event = new RideEvent(ride.getStatus().name(), ride.getId(),
                ride.getStatus(), message, LocalDateTime.now());
        template.convertAndSend("/topic/ride/" + ride.getId() + "/status", event);
    }

    public void newRideRequest(RideResponse ride) {
        template.convertAndSend("/topic/drivers/requests", ride);
    }


}
