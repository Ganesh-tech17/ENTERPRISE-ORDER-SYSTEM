package com.example.shop.inventory;

import jakarta.persistence.*;

import java.util.UUID;

/** What was reserved for an order - needed to compensate (release) if the saga rolls back. */
@Entity
@Table(name = "stock_reservations")
public class StockReservation {
    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean released;

    protected StockReservation() {}

    public StockReservation(UUID orderId, String sku, int quantity) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public void markReleased() { this.released = true; }
}
