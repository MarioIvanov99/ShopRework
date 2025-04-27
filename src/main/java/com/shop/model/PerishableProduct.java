package com.shop.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PerishableProduct extends Product {
    private int expirationDays;

    public PerishableProduct(String productId, String name, BigDecimal price, Category category, int expirationDays) {
        super(productId, name, price, category);
        this.expirationDays = expirationDays;
    }
}
