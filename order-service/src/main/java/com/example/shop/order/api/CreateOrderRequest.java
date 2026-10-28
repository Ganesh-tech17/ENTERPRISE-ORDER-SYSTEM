package com.example.shop.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(@NotNull Long userId, @NotEmpty @Valid List<Line> items) {
    public record Line(@NotBlank String sku, @Positive int quantity, @NotNull @Positive BigDecimal unitPrice) {}
}
