package com.shop.service;

import com.shop.exception.OutOfStockException;
import com.shop.exception.ProductNotFoundException;
import com.shop.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static junit.framework.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.*;

public class ShopServiceImplTestIntegration {

    private ShopServiceImpl shopServiceImpl;
    private Shop shop;
    private static ProductCatalog productCatalog;

    @BeforeAll
    public static void setup() {
        productCatalog = new ProductCatalog();

        PerishableProduct milk = new PerishableProduct(
                "MILK", "Fresh Milk", new BigDecimal("7.99"),
                Category.FOOD, 7);

        PerishableProduct yogurt = new PerishableProduct(
                "YOGURT", "Greek Yogurt", new BigDecimal("3.99"),
                Category.FOOD, 14);

        NonPerishableProduct cereal = new NonPerishableProduct(
                "CEREAL", "Breakfast Cereal", new BigDecimal("5.49"),
                Category.FOOD);

        productCatalog.getProductCatalog().put("MILK", milk);
        productCatalog.getProductCatalog().put("YOGURT", yogurt);
        productCatalog.getProductCatalog().put("CEREAL", cereal);
    }

    @BeforeEach
    public void setUp() {
        shopServiceImpl = new ShopServiceImpl();
        shop = new Shop();

        shop.setStockBatches(new HashMap<>());

        LocalDate testDate = LocalDate.of(2023, 5, 15);

        Batch milkBatch1 = new Batch(testDate.minusDays(8), 15);
        Batch milkBatch2 = new Batch(testDate.minusDays(3), 10);

        Batch yogurtBatch1 = new Batch(testDate.minusDays(16), 5);
        Batch yogurtBatch2 = new Batch(testDate.minusDays(5), 8);

        Batch cerealBatch = new Batch(testDate.minusDays(30), 20);

        PriorityQueue<Batch> milkBatches = new PriorityQueue<>();
        milkBatches.add(milkBatch1);
        milkBatches.add(milkBatch2);
        shop.getStockBatches().put("MILK", milkBatches);

        PriorityQueue<Batch> yogurtBatches = new PriorityQueue<>();
        yogurtBatches.add(yogurtBatch1);
        yogurtBatches.add(yogurtBatch2);
        shop.getStockBatches().put("YOGURT", yogurtBatches);

        PriorityQueue<Batch> cerealBatches = new PriorityQueue<>();
        cerealBatches.add(cerealBatch);
        shop.getStockBatches().put("CEREAL", cerealBatches);
    }

    @Test
    public void testClearExpiredFoodsClearsOnlyExpired_INTEGRATION() {
        try (MockedStatic<DateWrapper> mockedStatic = Mockito.mockStatic(DateWrapper.class)) {
            LocalDate testDate = LocalDate.of(2023, 5, 15);
            mockedStatic.when(DateWrapper::currentDate).thenReturn(testDate);

            shopServiceImpl.clearExpiredFoods(shop, productCatalog);

            assertEquals(1, shop.getStockBatches().get("MILK").size());
            assertEquals(1, shop.getStockBatches().get("YOGURT").size());
            assertEquals(1, shop.getStockBatches().get("CEREAL").size());

            Batch milkBatch = new Batch(testDate.minusDays(3), 15);
            Batch yogurtBatch = new Batch(testDate.minusDays(5), 8);

            for (Batch batch : shop.getStockBatches().get("MILK")) {
                assertFalse(batch.getArrivalDate().plusDays(7).isBefore(testDate));
                assertEquals(milkBatch.getArrivalDate(), batch.getArrivalDate());
            }

            for (Batch batch : shop.getStockBatches().get("YOGURT")) {
                assertFalse(batch.getArrivalDate().plusDays(14).isBefore(testDate));
                assertEquals(yogurtBatch.getArrivalDate(), batch.getArrivalDate());
            }
        }
    }

    @Test
    void testAddProductAddsProductToStock_INTEGRATION() {
        LocalDate testDate = LocalDate.of(2023, 5, 12);

        shopServiceImpl.addProduct(shop, "MILK", 15, testDate);

        assertEquals(3, shop.getStockBatches().get("MILK").size());
    }

    @Test
    void testRemoveProductThrowsProductNotFoundExceptionWhenStockIsEmpty_INTEGRATION() {
        Shop shop = new Shop();

        assertThrows(ProductNotFoundException.class, () -> shopServiceImpl.removeProduct(shop, "MILK", 1));
    }

    @Test
    void testRemoveProductThrowsOutOfStockExceptionWhenNotEnoughStock_INTEGRATION() {
        assertThrows(OutOfStockException.class, () -> shopServiceImpl.removeProduct(shop, "MILK", 30));
    }

    @Test
    void testRemoveProductRemovesProductFromStock_INTEGRATION() {
        shopServiceImpl.removeProduct(shop, "MILK", 10);
        assertEquals(2, shop.getStockBatches().get("MILK").size());
        assertEquals(15, shopServiceImpl.getProductQuantity(shop, "MILK"));

        shopServiceImpl.removeProduct(shop, "MILK", 5);
        assertEquals(1, shop.getStockBatches().get("MILK").size());
        assertEquals(10, shopServiceImpl.getProductQuantity(shop, "MILK"));
    }

    @Test
    void testCalculateCashierSalaryShouldReturnTheCorrectSalary_INTEGRATION() {
        Cashier cashier1 = new Cashier();
        Cashier cashier2 = new Cashier();
        Cashier cashier3 = new Cashier();

        cashier1.setSalary(BigDecimal.valueOf(1000));
        cashier2.setSalary(BigDecimal.valueOf(2000));
        cashier3.setSalary(BigDecimal.valueOf(3000));

        CashierDesk cashierDesk1 = new CashierDesk();
        CashierDesk cashierDesk2 = new CashierDesk();
        CashierDesk cashierDesk3 = new CashierDesk();

        cashierDesk1.setCashier(cashier1);
        cashierDesk2.setCashier(cashier2);
        cashierDesk3.setCashier(cashier3);

        List<CashierDesk> cashierDesks = new ArrayList<>();
        cashierDesks.add(cashierDesk1);
        cashierDesks.add(cashierDesk2);
        cashierDesks.add(cashierDesk3);

        shop.setCashierDesks(cashierDesks);

        BigDecimal expectedSalary = BigDecimal.valueOf(6000);

        BigDecimal actualSalary = shopServiceImpl.calculateCashierSalaries(shop);

        assertEquals(expectedSalary, actualSalary);
    }

    @Test
    void testProcessDeliveryShouldCorrectlyAddAllBatchesInDelivery_INTEGRATION() {
        LocalDate testDate = LocalDate.of(2023, 5, 20);
        Delivery delivery = new Delivery();
        Map<String, Batch> deliveryProducts = new HashMap<>();

        Batch milkBatch = new Batch(testDate, 15);
        Batch yogurtBatch = new Batch(testDate, 15);
        Batch legosBatch = new Batch(testDate, 15);

        deliveryProducts.put("MILK", milkBatch);
        deliveryProducts.put("YOGURT", yogurtBatch);
        deliveryProducts.put("LEGOS", legosBatch);

        delivery.setProducts(deliveryProducts);

        shopServiceImpl.processDelivery(shop, delivery);

        assertEquals(4, shop.getStockBatches().size());
        assertEquals(3, shop.getStockBatches().get("MILK").size());
        assertEquals(3, shop.getStockBatches().get("YOGURT").size());
        assertEquals(1, shop.getStockBatches().get("CEREAL").size());
        assertEquals(1, shop.getStockBatches().get("LEGOS").size());
    }

    @Test
    void testIsCloseToExpirationShouldReturnTrueWhenProductIsCloseToExpiration_INTEGRATION() {
        shop.setDaysBeforeExpityDiscount(7);

        assertTrue(shopServiceImpl.isCloseToExpire(shop, DateWrapper.currentDate().plusDays(6)));
        assertFalse(shopServiceImpl.isCloseToExpire(shop, DateWrapper.currentDate().plusDays(8)));
    }

    @Test
    void testGetProductPriceShouldReturnTheCorrectPriceAfterMarkup_INTEGRATION() {
        EnumMap<Category, BigDecimal> markupPercentage = new EnumMap<>(Category.class);
        markupPercentage.put(Category.FOOD, BigDecimal.valueOf(0.1));
        markupPercentage.put(Category.NON_FOOD, BigDecimal.valueOf(0.2));

        shop.setMarkupPercentage(markupPercentage);
        shop.setDaysBeforeExpityDiscount(7);
        shop.setDiscountPercentage(BigDecimal.valueOf(0.2));

        BigDecimal priceWithoutDiscount = shopServiceImpl.getProductPrice(shop, productCatalog, "MILK", DateWrapper.currentDate().plusDays(8));
        BigDecimal priceWithDiscount = shopServiceImpl.getProductPrice(shop, productCatalog, "MILK", DateWrapper.currentDate().plusDays(6));

        BigDecimal expectedPriceWithoutDiscount = new BigDecimal("8.79");
        BigDecimal expectedPriceWithDiscount = new BigDecimal("7.03");

        assertEquals(expectedPriceWithoutDiscount, priceWithoutDiscount);
        assertEquals(expectedPriceWithDiscount, priceWithDiscount);
    }
}

