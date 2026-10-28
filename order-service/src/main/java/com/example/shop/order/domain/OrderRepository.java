package com.example.shop.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant before);
}
