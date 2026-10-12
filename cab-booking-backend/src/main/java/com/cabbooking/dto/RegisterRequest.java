package com.cabbooking.dto;

import com.cabbooking.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest (
    @NotBlank
    String name,
    @NotBlank @Email
    String email,
    @NotBlank @Size(min = 6) String password,
    String phone,
    @NotNull
    Role role,
    String vehicleModel,
    String vehicleNumber
){}
