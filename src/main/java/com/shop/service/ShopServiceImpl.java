package com.shop.service;

import com.shop.exception.OutOfStockException;
import com.shop.exception.ProductNotFoundException;
import com.shop.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.PriorityQueue;

public class ShopServiceImpl implements ShopService {

    @Override
    public void clearExpiredFoods(Shop shop, ProductCatalog productCatalog) {
        LocalDate today = DateWrapper.currentDate();
        shop.getStockBatches().forEach((productId, batches) -> {
            Product product = productCatalog.getProductCatalog().get(productId);
            if (product instanceof PerishableProduct perishable) {
                batches.removeIf(batch -> batch.getArrivalDate().plusDays(perishable.getExpirationDays()).isBefore(today));
            }
        });
    }

    @Override
    public void addProduct(Shop shop, String productId, int amount, LocalDate arrivalDate) {
        PriorityQueue<Batch> stock = shop.getStockBatches().computeIfAbsent(productId, k -> new PriorityQueue<>());
        stock.add(new Batch(arrivalDate, amount));
    }

    @Override
    public void removeProduct(Shop shop, String productId, int amount) {
        PriorityQueue<Batch> stock = shop.getStockBatches().get(productId);
        if (stock == null || stock.isEmpty()) throw new ProductNotFoundException("Product not found");

        int remaining = amount;
        while (remaining > 0 && !stock.isEmpty()) {
            Batch batch = stock.poll();
            if (batch.getQuantity() > remaining) {
                batch.setQuantity(batch.getQuantity() - remaining);
                stock.add(batch);
                remaining = 0;
            } else {
                remaining -= batch.getQuantity();
            }
        }
        if (remaining > 0) throw new OutOfStockException("Not enough stock for product: " + productId);
    }

    public int getProductQuantity(Shop shop, String productId) {
        PriorityQueue<Batch> stock = shop.getStockBatches().get(productId);
        if (stock == null || stock.isEmpty()) throw new ProductNotFoundException("Product not found");
        return stock.stream().mapToInt(Batch::getQuantity).sum();
    }

    @Override
    public BigDecimal calculateCashierSalaries(Shop shop) {
        return shop.getCashierDesks().stream()
                .map(desk -> desk.getCashier().getSalary())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public void processDelivery(Shop shop, Delivery delivery) {
        for (Map.Entry<String, Batch> entry : delivery.getProducts().entrySet()) {
            String productId = entry.getKey();
            Batch batch = entry.getValue();
            addProduct(shop, productId, batch.getQuantity(), batch.getArrivalDate());
        }
    }

    @Override
    public BigDecimal getTotalCost(Shop shop, BigDecimal deliveryCost) {
        return deliveryCost.add(calculateCashierSalaries(shop));
    }

    @Override
    public BigDecimal getProductPrice(Shop shop, ProductCatalog productCatalog, String productId, LocalDate date) {
        Product product = productCatalog.getProductCatalog().get(productId);
        if (product == null) throw new ProductNotFoundException("Product not found");

        BigDecimal finalPrice = product.getPrice();
        finalPrice = finalPrice.multiply(shop.getMarkupPercentage().get(product.getCategory()).add(BigDecimal.ONE));

        if (isCloseToExpire(shop, date)) {
            finalPrice = finalPrice.multiply(BigDecimal.ONE.subtract(shop.getDiscountPercentage()));
        }
        return finalPrice.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getProfit(Shop shop, BigDecimal income, BigDecimal deliveryCost) {
        return income.subtract(getTotalCost(shop, deliveryCost)).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isCloseToExpire(Shop shop, LocalDate date) {
        return date.isBefore(DateWrapper.currentDate().plusDays(shop.getDaysBeforeExpityDiscount()));
    }
}
