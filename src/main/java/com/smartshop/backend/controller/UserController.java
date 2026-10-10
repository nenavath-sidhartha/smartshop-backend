package com.smartshop.backend.controller;

import com.smartshop.backend.dto.ApiResponse;
import com.smartshop.backend.dto.UserDTO;
import com.smartshop.backend.dto.UserProfileRequest;
import com.smartshop.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile(@RequestParam String email) {
        UserDTO user = userService.getUserProfile(email);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", user));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            @RequestParam String email,
            @RequestBody UserProfileRequest request
    ) {
        UserDTO updated = userService.updateUserProfile(email, request);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", updated));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users list retrieved", users));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserDTO>> toggleUserStatus(@PathVariable Long id) {
        UserDTO updated = userService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.success("User status updated", updated));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserDTO>> updateUserRole(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> request
    ) {
        String role = request != null ? request.get("role") : null;
        if (role == null || role.isBlank()) {
            throw new com.smartshop.backend.exception.BadRequestException("Role is required");
        }
        UserDTO updated = userService.updateUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", updated));
    }

    @PostMapping("/delivery-agent")
    public ResponseEntity<ApiResponse<UserDTO>> createDeliveryAgent(
            @jakarta.validation.Valid @RequestBody com.smartshop.backend.dto.RegisterRequest request
    ) {
        UserDTO agent = userService.createDeliveryAgent(request);
        return ResponseEntity.ok(ApiResponse.success("Delivery agent account created successfully", agent));
    }
}
