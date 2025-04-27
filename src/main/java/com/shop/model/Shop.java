package com.shop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Shop {
    private String name;
    private EnumMap<Category, BigDecimal> markupPercentage = new EnumMap<>(Category.class);;
    private int daysBeforeExpityDiscount;
    private BigDecimal discountPercentage;
    private List<CashierDesk> cashierDesks = new ArrayList<>();
    private Map<String, PriorityQueue<Batch>> stockBatches = new HashMap<>();
}