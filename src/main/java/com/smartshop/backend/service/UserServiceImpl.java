package com.smartshop.backend.service;

import com.smartshop.backend.dto.*;
import com.smartshop.backend.entity.Cart;
import com.smartshop.backend.entity.Role;
import com.smartshop.backend.entity.User;
import com.smartshop.backend.exception.BadRequestException;
import com.smartshop.backend.exception.ResourceNotFoundException;
import com.smartshop.backend.repository.CartRepository;
import com.smartshop.backend.repository.UserRepository;
import com.smartshop.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered. Please login.");
        }

        Role userRole = Role.ROLE_CUSTOMER;
        if (request.getRole() != null && request.getRole().equalsIgnoreCase("ADMIN")) {
            userRole = Role.ROLE_ADMIN;
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(userRole)
                .status("ACTIVE")
                .build();

        User savedUser = userRepository.save(user);

        // Initialize empty shopping cart for the user
        Cart cart = Cart.builder()
                .user(savedUser)
                .subtotal(0.0)
                .deliveryFee(0.0)
                .total(0.0)
                .build();
        cartRepository.save(cart);

        String token = jwtUtils.generateToken(savedUser.getEmail(), savedUser.getRole().name(), savedUser.getId());
        return new AuthResponse(token, mapToDTO(savedUser));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BadRequestException("Your account is deactivated. Please contact support.");
        }

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return new AuthResponse(token, mapToDTO(user));
    }

    @Override
    public UserDTO getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToDTO(user);
    }

    @Override
    public UserDTO updateUserProfile(String email, UserProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getNewPassword().length() < 6) {
                throw new BadRequestException("New password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        User updated = userRepository.save(user);
        return mapToDTO(updated);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new BadRequestException("Admin accounts cannot be deactivated");
        }

        user.setStatus("ACTIVE".equalsIgnoreCase(user.getStatus()) ? "INACTIVE" : "ACTIVE");
        User saved = userRepository.save(user);
        return mapToDTO(saved);
    }

    @Override
    public UserDTO updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role targetRole;
        try {
            String normalized = roleName.toUpperCase();
            if (!normalized.startsWith("ROLE_")) {
                normalized = "ROLE_" + normalized;
            }
            targetRole = Role.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role specified: " + roleName);
        }

        user.setRole(targetRole);
        User saved = userRepository.save(user);
        return mapToDTO(saved);
    }

    @Override
    public UserDTO createDeliveryAgent(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with this email already exists: " + request.getEmail());
        }

        User agent = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.ROLE_DELIVERY_AGENT)
                .status("ACTIVE")
                .build();

        User savedAgent = userRepository.save(agent);

        Cart cart = Cart.builder()
                .user(savedAgent)
                .subtotal(0.0)
                .deliveryFee(0.0)
                .total(0.0)
                .build();
        cartRepository.save(cart);

        return mapToDTO(savedAgent);
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
