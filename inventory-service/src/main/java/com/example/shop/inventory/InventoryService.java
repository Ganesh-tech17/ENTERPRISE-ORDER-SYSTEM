package com.example.shop.inventory;

import com.example.shop.common.events.Events.*;
import com.example.shop.common.events.Topics;
import com.example.shop.common.inbox.InboxService;
import com.example.shop.common.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final StockRepository stocks;
    private final ReservationRepository reservations;
    private final InboxService inbox;
    private final OutboxService outbox;

    public InventoryService(StockRepository stocks, ReservationRepository reservations,
                            InboxService inbox, OutboxService outbox) {
        this.stocks = stocks;
        this.reservations = reservations;
        this.inbox = inbox;
        this.outbox = outbox;
    }

    /** Saga step 1: reserve stock for a new order (all-or-nothing). */
    @Transactional
    public void onOrderCreated(OrderCreated event) {
        if (!inbox.firstTime(Topics.ORDER_CREATED + ":" + event.orderId())) {
            return; // duplicate delivery
        }
        Map<String, Integer> wanted = new TreeMap<>();
        event.items().forEach(i -> wanted.merge(i.sku(), i.quantity(), Integer::sum));

        Map<String, StockItem> stock = stocks.findForUpdate(wanted.keySet()).stream()
                .collect(Collectors.toMap(StockItem::getSku, Function.identity()));

        for (var entry : wanted.entrySet()) {
            StockItem item = stock.get(entry.getKey());
            if (item == null) {
                reject(event.orderId(), "Unknown SKU " + entry.getKey());
                return;
            }
            if (item.getAvailable() < entry.getValue()) {
                reject(event.orderId(), "Insufficient stock for " + entry.getKey());
                return;
            }
        }
        wanted.forEach((sku, qty) -> {
            stock.get(sku).decrease(qty);
            reservations.save(new StockReservation(event.orderId(), sku, qty));
        });
        outbox.publish(Topics.INVENTORY_RESERVED, event.orderId(),
                new InventoryReserved(event.orderId(), event.total()));
    }

    /** Compensation: give the reserved stock back when the order is cancelled (e.g. payment failed). */
    @Transactional
    public void onOrderCancelled(OrderCancelled event) {
        if (!inbox.firstTime(Topics.ORDER_CANCELLED + ":" + event.orderId())) {
            return;
        }
        for (StockReservation reservation : reservations.findByOrderIdAndReleasedFalse(event.orderId())) {
            stocks.findForUpdate(List.of(reservation.getSku()))
                    .forEach(s -> s.increase(reservation.getQuantity()));
            reservation.markReleased();
        }
    }

    private void reject(UUID orderId, String reason) {
        outbox.publish(Topics.INVENTORY_REJECTED, orderId, new InventoryRejected(orderId, reason));
    }
}
