package com.mphasis.tfp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="first_name",nullable = false)
    private String firstName;
    @Column(name="last_name",nullable = false)
    private  String lastName;
    @Column(name="username",nullable = false,unique = true)
    private String username;
    @Column(name="email",nullable = false,unique = true)
    private  String email;
    @Column(name="password",nullable = false)
    private String password;

}
