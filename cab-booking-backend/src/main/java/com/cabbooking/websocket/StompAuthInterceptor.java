package com.cabbooking.websocket;

import com.cabbooking.entity.Ride;
import com.cabbooking.repository.RideRepository;
import com.cabbooking.security.CustomUserDetailsService;
import com.cabbooking.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final Pattern RIDE_TOPIC = Pattern.compile("^/topic/ride/(\\d+)/.+$");

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RideRepository rideRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Missing token");
        }
        try {
            String token = header.substring(7);
            UserDetails user = userDetailsService.loadUserByUsername(jwtService.extractEmail(token));
            if (!jwtService.isValid(token, user)) {
                throw new MessagingException("Invalid token");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
            throw new MessagingException("Invalid token");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (!(principal instanceof Authentication auth)) {
            throw new MessagingException("Not authenticated");
        }
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new MessagingException("No destination");
        }

        if (destination.equals("/user/queue/errors")) {
            return;
        }
        if (destination.equals("/topic/drivers/requests")) {
            boolean isDriver = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"));
            if (!isDriver) {
                throw new MessagingException("Drivers only");
            }
            return;
        }

        Matcher m = RIDE_TOPIC.matcher(destination);
        if (m.matches()) {
            Ride ride = rideRepository.findById(Long.parseLong(m.group(1)))
                    .orElseThrow(() -> new MessagingException("Ride not found"));
            String email = auth.getName();
            boolean isRider = ride.getRider().getEmail().equals(email);
            boolean isDriver = ride.getDriver() != null
                    && ride.getDriver().getUser().getEmail().equals(email);
            if (!isRider && !isDriver) {
                throw new MessagingException("Not allowed");
            }
            return;
        }

        throw new MessagingException("Unknown destination");
    }
}