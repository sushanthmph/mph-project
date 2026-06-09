package com.mphasis.tfp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDTO {

    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "First name can only contain letters and spaces")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Last name can only contain letters and spaces")
    private String lastName;

    @NotBlank(message = "Username is required")
    @Pattern(
            regexp = "^\\S+$",
            message = "Username cannot contain spaces"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@._])[A-Za-z\\d!@._]{8,}$",
            message = "Password must be at least 8 characters with uppercase, lowercase, number, and special character (!@._)"
    )
    private String password;
}