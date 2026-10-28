package com.example.shop.inventory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class DataSeeder implements CommandLineRunner {
    private final StockRepository stocks;

    DataSeeder(StockRepository stocks) {
        this.stocks = stocks;
    }

    @Override
    public void run(String... args) {
        if (stocks.count() == 0) {
            stocks.save(new StockItem("SKU-1", 100));
            stocks.save(new StockItem("SKU-2", 50));
            stocks.save(new StockItem("SKU-3", 5));
        }
    }
}
