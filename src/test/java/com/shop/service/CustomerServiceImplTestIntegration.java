package com.shop.service;

import com.shop.exception.OutOfStockException;
import com.shop.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.PriorityQueue;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerServiceImplTestIntegration {



    CustomerServiceImpl customerServiceImpl;
    ShopServiceImpl shopServiceImpl;
    ProductCatalog productCatalog;
    Customer customer;
    Shop shop;

    @BeforeEach
    void setUp() {
        customerServiceImpl = new CustomerServiceImpl();
        shopServiceImpl = new ShopServiceImpl();
        customer = new Customer();
        shop = new Shop();

        productCatalog = new ProductCatalog();

        PerishableProduct milk = new PerishableProduct(
                "MILK", "Fresh Milk", new BigDecimal("7.99"),
                Category.FOOD, 7);

        productCatalog.getProductCatalog().put("MILK", milk);

        shop.setStockBatches(new HashMap<>());

        LocalDate testDate = LocalDate.of(2023, 5, 15);

        Batch milkBatch1 = new Batch(testDate.minusDays(8), 15);
        Batch milkBatch2 = new Batch(testDate.minusDays(3), 10);

        PriorityQueue<Batch> milkBatches = new PriorityQueue<>();
        milkBatches.add(milkBatch1);
        milkBatches.add(milkBatch2);
        shop.getStockBatches().put("MILK", milkBatches);
    }

    @Test
    void testAddProductToBasketThrowsOutOfStockExceptionWhenNotEnoughStock_INTEGRATION() {
        assertThrows(OutOfStockException.class, () -> customerServiceImpl.addProductToBasket(customer, shop, "MILK", 30, shopServiceImpl));
    }

    @Test
    void testAddProductToBasketAddsCorrectProductToBasket_INTEGRATION() {
        customerServiceImpl.addProductToBasket(customer, shop, "MILK", 16, shopServiceImpl);
        assertEquals(2, customer.getBasket().getItems().get("MILK").size());
        assertEquals(1, shop.getStockBatches().get("MILK").size());
        assertEquals(9, shopServiceImpl.getProductQuantity(shop, "MILK"));
    }
}