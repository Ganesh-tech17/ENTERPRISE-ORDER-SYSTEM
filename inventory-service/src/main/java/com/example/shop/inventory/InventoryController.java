package com.example.shop.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    public record StockRequest(@Min(0) int available) {}

    public record StockResponse(String sku, int available) {}

    private final StockRepository stocks;

    public InventoryController(StockRepository stocks) {
        this.stocks = stocks;
    }

    @GetMapping("/{sku}")
    public StockResponse get(@PathVariable String sku) {
        return stocks.findById(sku)
                .map(s -> new StockResponse(s.getSku(), s.getAvailable()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown SKU"));
    }

    @PutMapping("/{sku}")
    public StockResponse set(@PathVariable String sku, @Valid @RequestBody StockRequest request) {
        StockItem item = stocks.findById(sku).orElseGet(() -> new StockItem(sku, 0));
        item.setAvailable(request.available());
        stocks.save(item);
        return new StockResponse(sku, item.getAvailable());
    }
}
