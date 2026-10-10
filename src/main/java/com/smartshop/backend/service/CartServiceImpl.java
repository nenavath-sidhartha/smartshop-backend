package com.smartshop.backend.service;

import com.smartshop.backend.dto.AddToCartRequest;
import com.smartshop.backend.dto.CartDTO;
import com.smartshop.backend.dto.CartItemDTO;
import com.smartshop.backend.entity.Cart;
import com.smartshop.backend.entity.CartItem;
import com.smartshop.backend.entity.Product;
import com.smartshop.backend.entity.User;
import com.smartshop.backend.exception.BadRequestException;
import com.smartshop.backend.exception.ResourceNotFoundException;
import com.smartshop.backend.repository.CartItemRepository;
import com.smartshop.backend.repository.CartRepository;
import com.smartshop.backend.repository.ProductRepository;
import com.smartshop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartDTO getCartByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCartForUser(user));

        cart.recalculateTotals();
        cartRepository.save(cart);
        return mapToDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO getCartByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> createEmptyCartForUser(user));

        cart.recalculateTotals();
        cartRepository.save(cart);
        return mapToDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO addToCart(AddToCartRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> createEmptyCartForUser(user));

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

        int targetQuantity = request.getQuantity();
        if (existingItemOpt.isPresent()) {
            targetQuantity += existingItemOpt.get().getQuantity();
        }

        if (targetQuantity > product.getStock()) {
            throw new BadRequestException("Requested quantity (" + targetQuantity + ") exceeds available stock (" + product.getStock() + ")");
        }

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(targetQuantity);
            existingItem.setPrice(product.getPrice());
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .price(product.getPrice())
                    .build();
            cart.getItems().add(newItem);
        }

        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);
        return mapToDTO(savedCart);
    }

    @Override
    @Transactional
    public CartDTO updateCartItemQuantity(Long itemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));

        Cart cart = cartItem.getCart();

        if (quantity <= 0) {
            cart.getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            if (quantity > cartItem.getProduct().getStock()) {
                throw new BadRequestException("Quantity (" + quantity + ") exceeds available stock (" + cartItem.getProduct().getStock() + ")");
            }
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }

        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);
        return mapToDTO(savedCart);
    }

    @Override
    @Transactional
    public CartDTO removeCartItem(Long itemId) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));

        Cart cart = cartItem.getCart();
        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);
        return mapToDTO(savedCart);
    }

    @Override
    @Transactional
    public CartDTO clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart for user", "id", userId));

        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cart.setSubtotal(0.0);
        cart.setDeliveryFee(0.0);
        cart.setTotal(0.0);

        Cart savedCart = cartRepository.save(cart);
        return mapToDTO(savedCart);
    }

    private Cart createEmptyCartForUser(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .subtotal(0.0)
                .deliveryFee(0.0)
                .total(0.0)
                .build();
        return cartRepository.save(cart);
    }

    private CartDTO mapToDTO(Cart cart) {
        int totalItemsCount = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(cart.getItems().stream().map(this::mapItemToDTO).collect(Collectors.toList()))
                .subtotal(cart.getSubtotal())
                .deliveryFee(cart.getDeliveryFee())
                .total(cart.getTotal())
                .totalItemsCount(totalItemsCount)
                .build();
    }

    private CartItemDTO mapItemToDTO(CartItem item) {
        return CartItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .price(item.getPrice())
                .image(item.getProduct().getImage())
                .quantity(item.getQuantity())
                .availableStock(item.getProduct().getStock())
                .itemTotal(item.getPrice() * item.getQuantity())
                .build();
    }
}
