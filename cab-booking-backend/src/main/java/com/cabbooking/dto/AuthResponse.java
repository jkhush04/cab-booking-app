package com.cabbooking.dto;

import com.cabbooking.entity.Role;

public record AuthResponse(
        String token , String email, Role role
) {}
