package com.example.shop.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class DataSeeder implements CommandLineRunner {
    private final UserRepository repository;

    DataSeeder(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.save(new User("Demo User", "demo@example.com")); // gets id = 1
        }
    }
}
