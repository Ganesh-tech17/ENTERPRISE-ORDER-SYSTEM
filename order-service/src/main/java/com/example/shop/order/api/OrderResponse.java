package com.example.shop.order.api;

import com.example.shop.order.domain.Order;
import com.example.shop.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, Long userId, OrderStatus status, BigDecimal total,
                            String failureReason, Instant createdAt, List<Line> items) {

    public record Line(String sku, int quantity, BigDecimal unitPrice) {}

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getUserId(), o.getStatus(), o.getTotal(), o.getFailureReason(),
                o.getCreatedAt(),
                o.getItems().stream().map(l -> new Line(l.getSku(), l.getQuantity(), l.getUnitPrice())).toList());
    }
}
