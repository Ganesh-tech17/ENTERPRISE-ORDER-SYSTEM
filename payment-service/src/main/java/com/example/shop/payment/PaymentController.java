package com.example.shop.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    public record PaymentRequest(@NotNull UUID orderId, @NotNull @Positive BigDecimal amount) {}

    public record PaymentResponse(UUID id, UUID orderId, PaymentStatus status, BigDecimal amount,
                                  String failureReason, Instant createdAt) {
        static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getOrderId(), p.getStatus(), p.getAmount(),
                    p.getFailureReason(), p.getCreatedAt());
        }
    }

    private final PaymentService service;
    private final PaymentRepository repository;

    public PaymentController(PaymentService service, PaymentRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    /** POST /payments -> processes payment. 201 when created, 200 when the order was already paid (idempotent replay). */
    @PostMapping
    public ResponseEntity<PaymentResponse> process(@Valid @RequestBody PaymentRequest request) {
        PaymentService.Result result;
        try {
            result = service.process(request.orderId(), request.amount());
        } catch (DataIntegrityViolationException race) {
            // A concurrent request for the same order won the UNIQUE(order_id) race - return its payment.
            Payment existing = service.findByOrderId(request.orderId()).orElseThrow(() -> race);
            return ResponseEntity.ok(PaymentResponse.from(existing));
        }
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(PaymentResponse.from(result.payment()));
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    @GetMapping("/order/{orderId}")
    public PaymentResponse getByOrder(@PathVariable UUID orderId) {
        return service.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No payment for order"));
    }
}
