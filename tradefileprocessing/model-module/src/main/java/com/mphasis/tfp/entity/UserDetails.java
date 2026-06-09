package com.mphasis.tfp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "First name can only contain letters and spaces")
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Last name can only contain letters and spaces")
    private String lastName;

    @Column(name = "username", nullable = false, unique = true)
    @NotBlank(message = "Username is required")
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @Column(name = "password", nullable = false)
    @NotBlank(message = "Password is required")
    private String password;
}