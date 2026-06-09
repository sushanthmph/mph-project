package com.mphasis.tfp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "Username is required")
    @Pattern(
            regexp = "^\\S+$",
            message = "Username cannot contain spaces"
    )
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
