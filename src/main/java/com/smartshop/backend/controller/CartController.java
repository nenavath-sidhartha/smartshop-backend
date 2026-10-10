package com.smartshop.backend.controller;

import com.smartshop.backend.dto.AddToCartRequest;
import com.smartshop.backend.dto.ApiResponse;
import com.smartshop.backend.dto.CartDTO;
import com.smartshop.backend.dto.UpdateCartItemRequest;
import com.smartshop.backend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartDTO>> getCart(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String email
    ) {
        CartDTO cart;
        if (userId != null) {
            cart = cartService.getCartByUserId(userId);
        } else if (email != null) {
            cart = cartService.getCartByUserEmail(email);
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Either userId or email must be provided", 400));
        }
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartDTO>> addToCart(@Valid @RequestBody AddToCartRequest request) {
        CartDTO updatedCart = cartService.addToCart(request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", updatedCart));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartDTO>> updateQuantity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        CartDTO updatedCart = cartService.updateCartItemQuantity(id, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Cart item quantity updated", updatedCart));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartDTO>> removeItem(@PathVariable Long id) {
        CartDTO updatedCart = cartService.removeCartItem(id);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", updatedCart));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<CartDTO>> clearCart(@RequestParam Long userId) {
        CartDTO clearedCart = cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", clearedCart));
    }
}
