package com.shop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Basket {
    private Map<String, PriorityQueue<Batch>> items = new HashMap<>();
}
