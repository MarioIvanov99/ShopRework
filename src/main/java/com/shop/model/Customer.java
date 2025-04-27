package com.shop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class Customer {
    private BigDecimal budget;
    private Basket basket;

    public Customer() {
        this.budget = BigDecimal.ZERO;
        this.basket = new Basket();
    }
}