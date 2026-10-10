package com.smartshop.backend.service;

import com.smartshop.backend.dto.*;

import java.util.List;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserDTO getUserProfile(String email);
    UserDTO updateUserProfile(String email, UserProfileRequest request);
    List<UserDTO> getAllUsers();
    UserDTO toggleUserStatus(Long userId);
    UserDTO updateUserRole(Long userId, String role);
    UserDTO createDeliveryAgent(RegisterRequest request);
}
