package com.cabbooking.service;

import com.cabbooking.dto.*;
import com.cabbooking.entity.*;
import com.cabbooking.repository.*;
import com.cabbooking.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = userRepository.save(User.builder()
                .name(req.name())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .role(req.role())
                .build());

        if (req.role() == Role.DRIVER) {
            driverRepository.save(Driver.builder()
                    .user(user)
                    .vehicleModel(req.vehicleModel())
                    .vehicleNumber(req.vehicleNumber())
                    .build());
        }

        return new AuthResponse(jwtService.generateToken(user), user.getEmail(), user.getRole());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        User user = userRepository.findByEmail(req.email()).orElseThrow();
        return new AuthResponse(jwtService.generateToken(user), user.getEmail(), user.getRole());
    }
}