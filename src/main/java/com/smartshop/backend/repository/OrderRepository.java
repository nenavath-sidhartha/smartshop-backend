package com.smartshop.backend.repository;

import com.smartshop.backend.entity.Order;
import com.smartshop.backend.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByUserIdAndOrderStatusOrderByCreatedAtDesc(Long userId, OrderStatus orderStatus);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByOrderStatusInOrderByCreatedAtDesc(List<OrderStatus> statuses);
}
