package com.mphasis.tfp.controller;

import com.mphasis.tfp.dto.ApiResponseDTO;
import com.mphasis.tfp.dto.LoginRequestDTO;
import com.mphasis.tfp.dto.UserDetailsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Authentication", description = "APIs for user registration and login")
@RequestMapping("/auth")
public interface IAuthenticationController {

    @Operation(summary = "Register a new user",
            description = "Requires firstName, lastName, username, email and password")
    @PostMapping("/register")
    ApiResponseDTO<String> register(@RequestBody UserDetailsDTO request);

    @Operation(summary = "Login with username and password",
            description = "Returns a JWT token to use in Authorization header for all /Files/* endpoints")
    @PostMapping("/login")
    ApiResponseDTO<String> login(@RequestBody LoginRequestDTO request);

    @Operation(summary = "Logout",
            description = "Discard the JWT token on client side to logout")
    @PostMapping("/logout")
    ApiResponseDTO<String> logout();
}