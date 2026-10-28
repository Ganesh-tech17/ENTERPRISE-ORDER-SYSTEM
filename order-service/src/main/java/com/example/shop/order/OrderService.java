package com.example.shop.order;

import com.example.shop.order.api.CreateOrderRequest;
import com.example.shop.order.client.UserClient;
import com.example.shop.order.domain.IdempotencyRecord;
import com.example.shop.order.domain.IdempotencyRepository;
import com.example.shop.order.domain.Order;
import com.example.shop.order.domain.OrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orchestrates order creation. Deliberately NOT @Transactional: the remote User-service call must not
 * hold a DB transaction open, and a duplicate-key race has to be caught after the transaction rolled back.
 */
@Service
public class OrderService {

    public record CreateResult(Order order, boolean replayed) {}

    private final IdempotencyRepository idempotency;
    private final OrderRepository orders;
    private final UserClient userClient;
    private final OrderWriter writer;

    public OrderService(IdempotencyRepository idempotency, OrderRepository orders,
                        UserClient userClient, OrderWriter writer) {
        this.idempotency = idempotency;
        this.orders = orders;
        this.userClient = userClient;
        this.writer = writer;
    }

    public CreateResult create(String idempotencyKey, CreateOrderRequest request) {
        String scopedKey = request.userId() + ":" + idempotencyKey; // keys are per user
        String hash = hash(request);

        // 1. Fast path: we have seen this key before -> return the original result.
        Optional<IdempotencyRecord> prior = idempotency.findById(scopedKey);
        if (prior.isPresent()) {
            return replay(prior.get(), hash);
        }

        // 2. Validate the user (Retry + Circuit Breaker inside).
        userClient.assertUserExists(request.userId());

        // 3. Persist order + idempotency record + outbox event in ONE local transaction.
        try {
            return new CreateResult(writer.createOrder(scopedKey, hash, request), false);
        } catch (DataIntegrityViolationException race) {
            // Two identical requests raced; the PK on idempotency_keys let exactly one win.
            IdempotencyRecord winner = idempotency.findById(scopedKey).orElseThrow(() -> race);
            return replay(winner, hash);
        }
    }

    private CreateResult replay(IdempotencyRecord record, String hash) {
        if (!record.getRequestHash().equals(hash)) {
            throw new IdempotencyConflictException("Idempotency-Key was already used with a different request body");
        }
        Order order = orders.findById(record.getOrderId()).orElseThrow();
        return new CreateResult(order, true);
    }

    private static String hash(CreateOrderRequest request) {
        String canonical = request.userId() + "|" + request.items().stream()
                .map(i -> i.sku() + ":" + i.quantity() + ":" + i.unitPrice().stripTrailingZeros().toPlainString())
                .sorted()
                .collect(Collectors.joining(","));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
