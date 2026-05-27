package com.mphasis.tfp.services;

import com.mphasis.tfp.dto.LoginRequestDTO;
import com.mphasis.tfp.dto.UserDetailsDTO;

public interface IUserService {
    String register(UserDetailsDTO request);
    String login(LoginRequestDTO request);
}