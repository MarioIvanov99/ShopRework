package com.shop.service;

import com.shop.exception.OutOfStockException;
import com.shop.model.Basket;
import com.shop.model.Batch;
import com.shop.model.Customer;
import com.shop.model.Shop;

import java.util.PriorityQueue;

public class CustomerServiceImpl implements CustomerService {

    @Override
    public void addProductToBasket(Customer customer, Shop shop, String productId, int amount, ShopServiceImpl shopServiceImpl) {
        if(amount > shopServiceImpl.getProductQuantity(shop, productId)) throw new OutOfStockException("Not enough stock for product: " + productId);

        Basket basket = customer.getBasket();
        PriorityQueue<Batch> basketItems = basket.getItems().computeIfAbsent(productId, k -> new PriorityQueue<>());
        PriorityQueue<Batch> stock = shop.getStockBatches().get(productId);

        int remaining = amount;

        for(Batch batch : stock) {
            if(remaining > 0) {
                basketItems.add(batch);
                remaining -= batch.getQuantity();
            }
            else break;
        }

        shopServiceImpl.removeProduct(shop, productId, amount);
    }
}
