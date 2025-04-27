package com.shop.service;

import com.shop.exception.OutOfStockException;
import com.shop.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.PriorityQueue;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CustomerServiceImplTestUnit {

    CustomerServiceImpl customerServiceImpl;

    @Mock
    ShopServiceImpl shopServiceImpl;

    @Mock
    ProductCatalog productCatalog;

    @Mock
    Customer customer;

    @Mock
    Shop shop;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        customerServiceImpl = new CustomerServiceImpl();
    }

    @Test
    void testAddProductToBasketThrowsOutOfStockExceptionWhenNotEnoughStock_UNIT() {
        when(shopServiceImpl.getProductQuantity(shop, "MILK")).thenReturn(0);
        assertThrows(OutOfStockException.class, () -> customerServiceImpl.addProductToBasket(customer, shop, "MILK", 30, shopServiceImpl));
    }

    @Test
    void testAddProductToBasketAddsCorrectProductToBasket_UNIT() {
        Basket basket = new Basket();

        Batch batch1 = mock(Batch.class);
        Batch batch2 = mock(Batch.class);

        Map<String, PriorityQueue<Batch>> stockBatches = mock(Map.class);
        PriorityQueue<Batch> milkBatches = new PriorityQueue<>();
        milkBatches.add(batch1);
        milkBatches.add(batch2);

        when(shopServiceImpl.getProductQuantity(shop, "MILK")).thenReturn(25);
        when(customer.getBasket()).thenReturn(basket);
        when(shop.getStockBatches()).thenReturn(stockBatches);
        when(stockBatches.get("MILK")).thenReturn(milkBatches);

        when(batch1.getQuantity()).thenReturn(10);
        when(batch2.getQuantity()).thenReturn(10);
        Mockito.doNothing().when(shopServiceImpl).removeProduct(shop, "MILK", 15);

        customerServiceImpl.addProductToBasket(customer, shop, "MILK", 15, shopServiceImpl);

        assertEquals(2, basket.getItems().get("MILK").size());

        verify(shopServiceImpl).getProductQuantity(shop, "MILK");
        verify(customer).getBasket();
        verify(shop).getStockBatches();
        verify(stockBatches).get("MILK");
        verify(batch1).getQuantity();
        verify(batch2).getQuantity();
        verify(shopServiceImpl).removeProduct(shop, "MILK", 15);
    }
}