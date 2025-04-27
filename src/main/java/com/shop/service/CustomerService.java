package com.shop.service;

import com.shop.model.Customer;
import com.shop.model.Shop;

public interface CustomerService {
    void addProductToBasket(Customer customer, Shop shop, String productId, int amount, ShopServiceImpl shopServiceImpl);
}
