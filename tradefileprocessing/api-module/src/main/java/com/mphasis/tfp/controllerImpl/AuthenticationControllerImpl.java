package com.mphasis.tfp.controllerImpl;

import com.mphasis.tfp.controller.IAuthenticationController;
import com.mphasis.tfp.dto.ApiResponseDTO;
import com.mphasis.tfp.dto.LoginRequestDTO;
import com.mphasis.tfp.dto.UserDetailsDTO;
import com.mphasis.tfp.services.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthenticationControllerImpl implements IAuthenticationController {

    private final IUserService userService;

    @Override
    public ApiResponseDTO<String> register(UserDetailsDTO request) {
        log.info("API: Register request for username: {}", request.getUsername());
        String result = userService.register(request);
        return ApiResponseDTO.success(result, "Registration successful");
    }

    @Override
    public ApiResponseDTO<String> login(LoginRequestDTO request) {
        log.info("API: Login request for username: {}", request.getUsername());
        String token = userService.login(request);
        return ApiResponseDTO.success(token, "Login successful. Use this token in Authorization header as: Bearer <token>");
    }

    @Override
    public ApiResponseDTO<String> logout() {
        // JWT is stateless — logout is handled client side by discarding the token
        return ApiResponseDTO.success("Logged out successfully", "Please discard your token on client side.");
    }
}