package com.shop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedList;
import java.util.Queue;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CashierDesk {
    private Cashier cashier;
    private Queue<Customer> customers = new LinkedList<>();
}
