package com.shop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public abstract class Product {
    private String productId;
    private String name;
    private BigDecimal price;
    private Category category;
}
