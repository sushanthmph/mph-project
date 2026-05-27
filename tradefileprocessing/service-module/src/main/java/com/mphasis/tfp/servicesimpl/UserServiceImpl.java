package com.mphasis.tfp.servicesimpl;

import com.mphasis.tfp.dto.LoginRequestDTO;
import com.mphasis.tfp.dto.UserDetailsDTO;
import com.mphasis.tfp.entity.UserDetails;
import com.mphasis.tfp.repository.UserRepository;
import com.mphasis.tfp.services.IUserService;
import com.mphasis.tfp.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String register(UserDetailsDTO request) {
        log.info("Register request received for username: {}", request.getUsername());

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required.");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required.");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            log.error("Username already taken: {}", request.getUsername());
            throw new IllegalStateException("Username '" + request.getUsername() + "' is already taken.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.error("Email already registered: {}", request.getEmail());
            throw new IllegalStateException("Email '" + request.getEmail() + "' is already registered.");
        }

        UserDetails user = new UserDetails();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        // Encrypt password before saving
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
        log.info("User registered successfully: {}", request.getUsername());

        return "User '" + request.getUsername() + "' registered successfully.";
    }

    @Override
    public String login(LoginRequestDTO request) {
        log.info("Login request received for username: {}", request.getUsername());

        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        UserDetails user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found: {}", request.getUsername());
                    return new NoSuchElementException("Invalid username or password.");
                });

        // Verify encrypted password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.error("Invalid password for username: {}", request.getUsername());
            throw new IllegalArgumentException("Invalid username or password.");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername());
        log.info("JWT token generated for user: {}", request.getUsername());

        return token;
    }
}