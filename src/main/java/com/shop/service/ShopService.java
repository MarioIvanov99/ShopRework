package com.shop.service;

import com.shop.exception.OutOfStockException;
import com.shop.exception.ProductNotFoundException;
import com.shop.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ShopService {
    void clearExpiredFoods(Shop shop, ProductCatalog productCatalog);

    void addProduct(Shop shop, String productId, int amount, LocalDate arrivalDate);

    void removeProduct(Shop shop, String productId, int amount);

    BigDecimal calculateCashierSalaries(Shop shop);

    void processDelivery(Shop shop, Delivery delivery);

    BigDecimal getTotalCost(Shop shop, BigDecimal deliveryCost);

    BigDecimal getProductPrice(Shop shop, ProductCatalog productCatalog, String productId, LocalDate date);

    BigDecimal getProfit(Shop shop, BigDecimal income, BigDecimal deliveryCost);
}
