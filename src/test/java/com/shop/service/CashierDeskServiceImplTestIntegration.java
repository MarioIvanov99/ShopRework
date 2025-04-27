package com.shop.service;

import com.shop.exception.InsufficientFundsException;
import com.shop.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static junit.framework.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;


class CashierDeskServiceImplTestIntegration {
    private CashierDeskServiceImpl cashierDeskServiceImpl;
    private ShopServiceImpl shopServiceImpl;
    private Shop shop;
    private ProductCatalog productCatalog;
    private Basket basket;
    private Customer customer;
    private CashierDesk cashierDesk;
    private Cashier cashier;

    @BeforeEach
    void setUp() {
        shopServiceImpl = new ShopServiceImpl();
        cashierDeskServiceImpl = new CashierDeskServiceImpl(shopServiceImpl);

        productCatalog = new ProductCatalog();
        Product milk = new PerishableProduct("MILK", "Milk", BigDecimal.valueOf(2.99), Category.FOOD, 7);
        Product bread = new PerishableProduct("BREAD", "Bread", BigDecimal.valueOf(1.99), Category.FOOD, 3);
        Product beans = new NonPerishableProduct("BEANS", "Beans", BigDecimal.valueOf(0.99), Category.FOOD);

        productCatalog.getProductCatalog().put("MILK", milk);
        productCatalog.getProductCatalog().put("BREAD", bread);
        productCatalog.getProductCatalog().put("BEANS", beans);

        shop = new Shop();
        shop.setName("TestShop");
        shop.setDaysBeforeExpityDiscount(7);
        shop.setDiscountPercentage(BigDecimal.valueOf(0.20));
        shop.getMarkupPercentage().put(Category.FOOD, BigDecimal.valueOf(0.10));
        shop.getMarkupPercentage().put(Category.NON_FOOD, BigDecimal.valueOf(0.20));

        LocalDate today = DateWrapper.currentDate();
        shopServiceImpl.addProduct(shop, "MILK", 10, today.minusDays(3));
        shopServiceImpl.addProduct(shop, "BREAD", 5, today.minusDays(1));
        shopServiceImpl.addProduct(shop, "BEANS", 20, today);

        cashier = new Cashier();
        cashier.setId("C001");
        cashier.setName("John Doe");
        cashier.setSalary(BigDecimal.valueOf(2000));

        cashierDesk = new CashierDesk();
        cashierDesk.setCashier(cashier);

        shop.getCashierDesks().add(cashierDesk);

        customer = new Customer();
        customer.setBudget(BigDecimal.valueOf(100));

        basket = new Basket();
    }

    @Test
    void testGetTotalBasketPriceReturnsCorrectTotal_INTEGRATION() {
        PriorityQueue<Batch> milkBatches = new PriorityQueue<>();
        milkBatches.add(new Batch(DateWrapper.currentDate().minusDays(3), 2));

        PriorityQueue<Batch> breadBatches = new PriorityQueue<>();
        breadBatches.add(new Batch(DateWrapper.currentDate().minusDays(1), 1));

        basket.getItems().put("MILK", milkBatches);
        basket.getItems().put("BREAD", breadBatches);

        BigDecimal expectedMilkPrice = BigDecimal.valueOf(2.99)
                .multiply(BigDecimal.valueOf(1.10))
                .multiply(BigDecimal.valueOf(0.80))
                .multiply(BigDecimal.valueOf(2))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal expectedBreadPrice = BigDecimal.valueOf(1.99)
                .multiply(BigDecimal.valueOf(1.10))
                .multiply(BigDecimal.valueOf(0.80))
                .multiply(BigDecimal.valueOf(1))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal expectedTotal = expectedMilkPrice.add(expectedBreadPrice);

        BigDecimal actualTotal = cashierDeskServiceImpl.getTotalBasketPrice(shop, productCatalog, basket);

        assertEquals(expectedTotal, actualTotal);
    }

    @Test
    void testProcessPurchaseThrowsExceptionWhenInsufficientFunds_INTEGRATION() {
        PriorityQueue<Batch> milkBatches = new PriorityQueue<>();
        milkBatches.add(new Batch(DateWrapper.currentDate().minusDays(3), 20));

        basket.getItems().put("MILK", milkBatches);

        BigDecimal lowBudget = BigDecimal.valueOf(10);

        assertThrows(InsufficientFundsException.class, () -> cashierDeskServiceImpl.processPurchase(shop, productCatalog, basket, lowBudget, cashierDesk));

        int milkQuantity = shopServiceImpl.getProductQuantity(shop, "MILK");
        assertEquals(30, milkQuantity);
    }

    @Test
    void testProcessPurchaseCreatesReceiptOnSuccess_INTEGRATION() {
        PriorityQueue<Batch> beansBatches = new PriorityQueue<>();
        beansBatches.add(new Batch(DateWrapper.currentDate(), 2));

        basket.getItems().put("BEANS", beansBatches);

        BigDecimal sufficientBudget = BigDecimal.valueOf(5);

        cashierDeskServiceImpl.processPurchase(shop, productCatalog, basket, sufficientBudget, cashierDesk);
    }

    @Test
    void testProcessPurchaseForAllCustomersProcessesEachCustomer_INTEGRATION() {
        Customer customer1 = new Customer();
        customer1.setBudget(BigDecimal.valueOf(50));

        Customer customer2 = new Customer();
        customer2.setBudget(BigDecimal.valueOf(30));

        PriorityQueue<Batch> milkBatches1 = new PriorityQueue<>();
        milkBatches1.add(new Batch(DateWrapper.currentDate().minusDays(3), 1));
        customer1.getBasket().getItems().put("MILK", milkBatches1);

        PriorityQueue<Batch> breadBatches = new PriorityQueue<>();
        breadBatches.add(new Batch(DateWrapper.currentDate().minusDays(1), 2));
        customer2.getBasket().getItems().put("BREAD", breadBatches);

        List<Customer> customers = new ArrayList<>();
        customers.add(customer1);
        customers.add(customer2);

        cashierDeskServiceImpl.processPurchaseForAllCustomers(shop, productCatalog, customers, cashierDesk);
    }

    @Test
    void testGetTotalCustomerSpendingCalculatesCorrectly_INTEGRATION() {
        Customer customer1 = new Customer();
        Customer customer2 = new Customer();

        PriorityQueue<Batch> milkBatches1 = new PriorityQueue<>();
        milkBatches1.add(new Batch(DateWrapper.currentDate().minusDays(3), 1));
        customer1.getBasket().getItems().put("MILK", milkBatches1);

        PriorityQueue<Batch> breadBatches = new PriorityQueue<>();
        breadBatches.add(new Batch(DateWrapper.currentDate().minusDays(1), 2));
        customer2.getBasket().getItems().put("BREAD", breadBatches);

        List<Customer> customers = new ArrayList<>();
        customers.add(customer1);
        customers.add(customer2);

        BigDecimal expectedMilkPrice = BigDecimal.valueOf(2.99)
                .multiply(BigDecimal.valueOf(1.10))
                .multiply(BigDecimal.valueOf(0.80))
                .multiply(BigDecimal.valueOf(1))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal expectedBreadPrice = BigDecimal.valueOf(1.99)
                .multiply(BigDecimal.valueOf(1.10))
                .multiply(BigDecimal.valueOf(0.80))
                .multiply(BigDecimal.valueOf(2))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal expectedTotal = expectedMilkPrice.add(expectedBreadPrice);

        BigDecimal actualTotal = cashierDeskServiceImpl.getTotalCustomerSpending(shop, productCatalog, customers);

        assertEquals(expectedTotal, actualTotal);
    }

    @Test
    void testCreateReceiptReturnsValidReceipt_INTEGRATION() {
        PriorityQueue<Batch> beansBatches = new PriorityQueue<>();
        beansBatches.add(new Batch(DateWrapper.currentDate(), 3));

        basket.getItems().put("BEANS", beansBatches);

        Receipt receipt = cashierDeskServiceImpl.createReceipt(cashier, shop, productCatalog, basket);

        assertNotNull(receipt);
        assertEquals(cashier.getName(), receipt.getCashierName());
        assertEquals(DateWrapper.currentDate(), receipt.getDate());

        Map<String, Integer> items = receipt.getItems();
        assertTrue(items.containsKey("BEANS"));
        assertEquals(3, items.get("BEANS").intValue());

        BigDecimal expectedBeansPrice = BigDecimal.valueOf(0.99)
                .multiply(BigDecimal.valueOf(1.1))
                .multiply(BigDecimal.valueOf(3))
                .multiply(BigDecimal.valueOf(0.8))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(expectedBeansPrice, receipt.getTotal());
    }
}