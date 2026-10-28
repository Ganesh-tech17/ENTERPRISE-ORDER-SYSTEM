package com.example.shop.inventory;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_items")
public class StockItem {
    @Id
    private String sku;

    @Column(nullable = false)
    private int available;

    protected StockItem() {}

    public StockItem(String sku, int available) {
        this.sku = sku;
        this.available = available;
    }

    public String getSku() { return sku; }
    public int getAvailable() { return available; }
    public void setAvailable(int available) { this.available = available; }
    public void decrease(int qty) { this.available -= qty; }
    public void increase(int qty) { this.available += qty; }
}
