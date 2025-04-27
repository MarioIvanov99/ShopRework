package com.shop.service;

import com.shop.exception.OutOfStockException;
import com.shop.exception.ProductNotFoundException;
import com.shop.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ShopServiceImplTestUnit {

    private ShopServiceImpl shopServiceImpl, mockShopServiceImpl;

    @Mock
    private Shop shop;

    @Mock
    private ProductCatalog productCatalog;

    @Mock
    private PerishableProduct perishableProduct;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        shopServiceImpl = new ShopServiceImpl();
        mockShopServiceImpl = Mockito.spy(shopServiceImpl);
    }

    @Test
    void testAddProductAddsProductToStock_UNIT() {
        String productId = "P1";
        Map<String, PriorityQueue<Batch>> stockBatches = mock(Map.class);
        PriorityQueue<Batch> batches = mock(PriorityQueue.class);

        when(shop.getStockBatches()).thenReturn(stockBatches);
        when(stockBatches.computeIfAbsent(anyString(), any())).thenReturn(batches);
        when(batches.add(any())).thenReturn(true);

        shopServiceImpl.addProduct(shop, productId, 5, LocalDate.now());

        verify(shop).getStockBatches();
        verify(stockBatches).computeIfAbsent(anyString(), any());
        verify(batches).add(any());
    }


    @Test
    void testClearExpiredFoodsClearsOnlyExpired_UNIT() {
        try (MockedStatic<DateWrapper> mockedStatic = Mockito.mockStatic(DateWrapper.class)) {
            Map<String, PriorityQueue<Batch>> stockBatches = new HashMap<>();
            Map<String, Product> productMap = new HashMap<>();

            LocalDate testDate = LocalDate.of(2023, 5, 15);
            mockedStatic.when(DateWrapper::currentDate).thenReturn(testDate);

            String perishableProductId = "P1";
            when(perishableProduct.getExpirationDays()).thenReturn(5);

            Batch expiredBatch = mock(Batch.class);
            when(expiredBatch.getArrivalDate()).thenReturn(testDate.minusDays(6)); // Expired

            Batch freshBatch = mock(Batch.class);
            when(freshBatch.getArrivalDate()).thenReturn(testDate.minusDays(3)); // Not expired

            PriorityQueue<Batch> batches = new PriorityQueue<>(Comparator.comparing(Batch::getArrivalDate));
            batches.add(expiredBatch);
            batches.add(freshBatch);

            stockBatches.put(perishableProductId, batches);
            productMap.put(perishableProductId, perishableProduct);

            when(shop.getStockBatches()).thenReturn(stockBatches);
            when(productCatalog.getProductCatalog()).thenReturn(productMap);

            shopServiceImpl.clearExpiredFoods(shop, productCatalog);

            assertEquals(1, stockBatches.get(perishableProductId).size());
            assertTrue(stockBatches.get(perishableProductId).contains(freshBatch));
            assertFalse(stockBatches.get(perishableProductId).contains(expiredBatch));

            verify(shop).getStockBatches();
            verify(productCatalog).getProductCatalog();
            verify(perishableProduct, atLeastOnce()).getExpirationDays();
            verify(expiredBatch, atLeastOnce()).getArrivalDate();
            verify(freshBatch, atLeastOnce()).getArrivalDate();
        }
    }

    @Test
    void testRemoveProductThrowsProductNotFoundExceptionWhenStockIsEmpty_UNIT() {
        Map<String, PriorityQueue<Batch>> stockBatches = new HashMap<>();
        when(shop.getStockBatches()).thenReturn(stockBatches);

        assertThrows(ProductNotFoundException.class, () -> shopServiceImpl.removeProduct(shop, "P1", 1));

        verify(shop).getStockBatches();
    }

    @Test
    void testRemoveProductThrowsOutOfStockExceptionWhenNotEnoughStock_UNIT() {
        String productId = "P1";
        PriorityQueue<Batch> mockStock = mock(PriorityQueue.class);
        Map<String, PriorityQueue<Batch>> stockBatches = mock(Map.class);
        when(shop.getStockBatches()).thenReturn(stockBatches);
        when(stockBatches.get(productId)).thenReturn(mockStock);

        Batch batch1 = mock(Batch.class);
        when(batch1.getQuantity()).thenReturn(5);

        Batch batch2 = mock(Batch.class);
        when(batch2.getQuantity()).thenReturn(3);

        when(mockStock.isEmpty()).thenReturn(false).thenReturn(false).thenReturn(true);
        when(mockStock.poll()).thenReturn(batch1).thenReturn(batch2);

        assertThrows(OutOfStockException.class, () -> shopServiceImpl.removeProduct(shop, productId, 10));

        verify(shop).getStockBatches();
        verify(stockBatches).get(productId);
        verify(mockStock, times(3)).isEmpty();
        verify(mockStock).poll();
    }

    @Test
    void testRemoveProductRemovesProductFromStock_UNIT() {
        String productId = "P1";
        PriorityQueue<Batch> mockStock = mock(PriorityQueue.class);
        Map<String, PriorityQueue<Batch>> stockBatches = mock(Map.class);
        when(shop.getStockBatches()).thenReturn(stockBatches);
        when(stockBatches.get(productId)).thenReturn(mockStock);

        Batch batch1 = mock(Batch.class);
        when(batch1.getQuantity()).thenReturn(5);

        Batch batch2 = mock(Batch.class);
        when(batch2.getQuantity()).thenReturn(3);

        when(mockStock.isEmpty()).thenReturn(false).thenReturn(false);
        when(mockStock.poll()).thenReturn(batch1).thenReturn(batch2);

        assertDoesNotThrow(() -> shopServiceImpl.removeProduct(shop, productId, 8));

        verify(shop).getStockBatches();
        verify(stockBatches).get(productId);
        verify(mockStock, times(3)).isEmpty();
        verify(mockStock, times(2)).poll();
    }
    @Test
    void testProcessDeliveryShouldCorrectlyAddAllBatchesInDelivery_UNIT() {
        String productId1 = "P1";
        String productId2 = "P2";

        int quantity = 1;

        LocalDate testDate = LocalDate.of(2023, 5, 15);

        Map<String, Batch> products = new HashMap<>();
        Batch batch1 = new Batch(testDate, quantity);
        Batch batch2 = new Batch(testDate, quantity);
        products.put(productId1, batch1);
        products.put(productId2, batch2);

        Delivery delivery = mock(Delivery.class);
        when(delivery.getProducts()).thenReturn(products);
        Mockito.doNothing().when(mockShopServiceImpl).addProduct(eq(shop), anyString(), anyInt(), any());

        mockShopServiceImpl.processDelivery(shop, delivery);

        verify(delivery).getProducts();
        verify(mockShopServiceImpl, times(2)).addProduct(eq(shop), anyString(), anyInt(), any());
    }

    @Test
    void testGetProductPriceShouldReturnTheCorrectPriceAfterMarkup_UNIT() {
        EnumMap<Category, BigDecimal> markupPercentage = new EnumMap<>(Category.class);
        markupPercentage.put(Category.FOOD, BigDecimal.valueOf(0.1));
        markupPercentage.put(Category.NON_FOOD, BigDecimal.valueOf(0.2));

        Map<String, Product> mockProductCatalog = mock(Map.class);

        Product product = mock(Product.class);

        LocalDate testDate = LocalDate.of(2023, 5, 15);

        when(productCatalog.getProductCatalog()).thenReturn(mockProductCatalog);
        when(mockProductCatalog.get(anyString())).thenReturn(product);

        when(product.getPrice()).thenReturn(BigDecimal.valueOf(10));
        when(product.getCategory()).thenReturn(Category.FOOD);
        when(shop.getMarkupPercentage()).thenReturn(markupPercentage);
        when(shop.getDiscountPercentage()).thenReturn(BigDecimal.valueOf(0.2));
        Mockito.doReturn(true).when(mockShopServiceImpl).isCloseToExpire(shop, testDate);

        BigDecimal expectedPrice = BigDecimal.valueOf(10).multiply(BigDecimal.valueOf(1.1)).multiply(BigDecimal.valueOf(0.8));

        BigDecimal actualPrice = mockShopServiceImpl.getProductPrice(shop, productCatalog, "P1", testDate);

        assertEquals(expectedPrice, actualPrice);

        verify(productCatalog).getProductCatalog();
        verify(product).getPrice();
        verify(product).getCategory();
        verify(shop).getMarkupPercentage();
        verify(shop).getDiscountPercentage();
        verify(mockShopServiceImpl).isCloseToExpire(shop, testDate);
    }
}