package com.smartshop.backend.service;

import com.smartshop.backend.dto.AddToCartRequest;
import com.smartshop.backend.dto.CartDTO;

public interface CartService {
    CartDTO getCartByUserId(Long userId);
    CartDTO getCartByUserEmail(String email);
    CartDTO addToCart(AddToCartRequest request);
    CartDTO updateCartItemQuantity(Long itemId, Integer quantity);
    CartDTO removeCartItem(Long itemId);
    CartDTO clearCart(Long userId);
}
