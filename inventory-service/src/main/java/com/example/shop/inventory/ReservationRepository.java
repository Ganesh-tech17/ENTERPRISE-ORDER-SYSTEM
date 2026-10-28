package com.example.shop.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<StockReservation, UUID> {
    List<StockReservation> findByOrderIdAndReleasedFalse(UUID orderId);
}
