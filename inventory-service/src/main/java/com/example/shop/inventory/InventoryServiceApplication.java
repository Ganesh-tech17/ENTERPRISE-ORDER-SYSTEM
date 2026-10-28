package com.example.shop.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Scan the whole "com.example.shop" tree so the shared outbox/inbox classes in :common are picked up.
@SpringBootApplication(scanBasePackages = "com.example.shop")
@EntityScan("com.example.shop")
@EnableJpaRepositories("com.example.shop")
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
