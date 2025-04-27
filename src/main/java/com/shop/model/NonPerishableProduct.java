package com.shop.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class NonPerishableProduct extends Product {
    public NonPerishableProduct(String productId, String name, BigDecimal price, Category category) {
        super(productId, name, price, category);
    }
}
