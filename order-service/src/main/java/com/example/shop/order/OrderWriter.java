package com.example.shop.order;

import com.example.shop.common.events.Events;
import com.example.shop.common.events.Events.*;
import com.example.shop.common.events.Topics;
import com.example.shop.common.inbox.InboxService;
import com.example.shop.common.outbox.OutboxService;
import com.example.shop.order.api.CreateOrderRequest;
import com.example.shop.order.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/** All transactional state changes of the Order aggregate + the saga reactions. */
@Component
public class OrderWriter {

    private final OrderRepository orders;
    private final IdempotencyRepository idempotency;
    private final OutboxService outbox;
    private final InboxService inbox;

    public OrderWriter(OrderRepository orders, IdempotencyRepository idempotency,
                       OutboxService outbox, InboxService inbox) {
        this.orders = orders;
        this.idempotency = idempotency;
        this.outbox = outbox;
        this.inbox = inbox;
    }

    @Transactional
    public Order createOrder(String scopedKey, String requestHash, CreateOrderRequest request) {
        List<OrderLine> lines = request.items().stream()
                .map(i -> new OrderLine(i.sku(), i.quantity(), i.unitPrice()))
                .toList();
        BigDecimal total = lines.stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Order order = new Order(request.userId(), lines, total);
        orders.save(order);
        // flush now so a duplicate key surfaces HERE and rolls back the order + outbox row together
        idempotency.saveAndFlush(new IdempotencyRecord(scopedKey, requestHash, order.getId()));

        outbox.publish(Topics.ORDER_CREATED, order.getId(), new OrderCreated(order.getId(), order.getUserId(), total,
                request.items().stream().map(i -> new Events.Item(i.sku(), i.quantity())).toList()));
        return order;
    }

    // ---------------- saga reactions (each one idempotent via inbox + state guard) ----------------

    @Transactional
    public void onPaymentCompleted(PaymentCompleted event) {
        if (!inbox.firstTime(Topics.PAYMENT_COMPLETED + ":" + event.orderId())) return;
        orders.findById(event.orderId()).ifPresent(Order::confirm);
    }

    /** Saga rollback: payment failed -> cancel the order and tell Inventory to release the reserved stock. */
    @Transactional
    public void onPaymentFailed(PaymentFailed event) {
        if (!inbox.firstTime(Topics.PAYMENT_FAILED + ":" + event.orderId())) return;
        orders.findById(event.orderId()).ifPresent(order -> {
            if (order.cancel("Payment failed: " + event.reason())) {
                outbox.publish(Topics.ORDER_CANCELLED, order.getId(), new OrderCancelled(order.getId(), event.reason()));
            }
        });
    }

    /** Nothing was reserved, so there is nothing to compensate - just cancel. */
    @Transactional
    public void onInventoryRejected(InventoryRejected event) {
        if (!inbox.firstTime(Topics.INVENTORY_REJECTED + ":" + event.orderId())) return;
        orders.findById(event.orderId()).ifPresent(order -> order.cancel("Inventory rejected: " + event.reason()));
    }

    /** Saga timeout: an order stuck in PENDING (e.g. Kafka or a service was down too long) is cancelled + compensated. */
    @Transactional
    public int cancelStale(Instant olderThan) {
        int cancelled = 0;
        for (Order order : orders.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, olderThan)) {
            if (order.cancel("Timed out waiting for payment/inventory")) {
                outbox.publish(Topics.ORDER_CANCELLED, order.getId(), new OrderCancelled(order.getId(), "timeout"));
                cancelled++;
            }
        }
        return cancelled;
    }
}
