package com.example.shop.order.api;

import com.example.shop.order.OrderService;
import com.example.shop.order.domain.OrderRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;
    private final OrderRepository repository;

    public OrderController(OrderService service, OrderRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    /**
     * POST /orders -> creates an order (status PENDING; the saga confirms or cancels it asynchronously).
     * Requires an Idempotency-Key header. Retrying with the same key returns the original order with 200.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                @Valid @RequestBody CreateOrderRequest request) {
        var result = service.create(idempotencyKey, request);
        OrderResponse body = OrderResponse.from(result.order());
        return ResponseEntity
                .status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Location", "/orders/" + body.id())
                .header("Idempotent-Replayed", String.valueOf(result.replayed()))
                .body(body);
    }

    /** GET /orders/{id} -> fetch order (poll this to watch PENDING -> CONFIRMED / CANCELLED). */
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}
