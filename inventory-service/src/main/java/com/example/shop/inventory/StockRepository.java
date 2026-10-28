package com.example.shop.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StockRepository extends JpaRepository<StockItem, String> {

    /** Row locks, always taken in SKU order, so concurrent reservations cannot deadlock or oversell. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockItem s where s.sku in :skus order by s.sku")
    List<StockItem> findForUpdate(@Param("skus") Collection<String> skus);
}
