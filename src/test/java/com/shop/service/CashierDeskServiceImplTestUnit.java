package com.shop.service;

import com.shop.exception.InsufficientFundsException;
import com.shop.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CashierDeskServiceImplTestUnit {
    private CashierDeskServiceImpl cashierDeskServiceImpl;

    @Mock
    private CashierDeskServiceImpl mockCashierDeskServiceImpl;

    @Mock
    private ShopServiceImpl shopServiceImpl;

    @Mock
    private Shop shop;

    @Mock
    private ProductCatalog productCatalog;

    @Mock
    private Basket basket;

    @Mock
    private CashierDesk cashierDesk;

    @Mock
    private Cashier cashier;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        cashierDeskServiceImpl = new CashierDeskServiceImpl(shopServiceImpl);
        mockCashierDeskServiceImpl = Mockito.spy(cashierDeskServiceImpl);

        when(cashierDesk.getCashier()).thenReturn(cashier);
    }

    @Test
    void testGetTotalBasketPriceReturnsCorrectTotal_UNIT() {
        String productId = "P1";
        Product product = mock(Product.class);
        when(productCatalog.getProductCatalog()).thenReturn(Map.of(productId, product));

        Batch batch1 = new Batch(LocalDate.now(), 2);
        Batch batch2 = new Batch(LocalDate.now(), 3);
        PriorityQueue<Batch> batches = new PriorityQueue<>(Comparator.comparing(Batch::getArrivalDate));
        batches.add(batch1);
        batches.add(batch2);
        when(basket.getItems()).thenReturn(Map.of(productId, batches));

        when(shopServiceImpl.getProductPrice(eq(shop), eq(productCatalog), eq(productId), any())).thenReturn(BigDecimal.valueOf(10));

        BigDecimal total = cashierDeskServiceImpl.getTotalBasketPrice(shop, productCatalog, basket);

        assertEquals(BigDecimal.valueOf(50).setScale(2), total);

        verify(productCatalog).getProductCatalog();
        verify(shopServiceImpl, times(2)).getProductPrice(eq(shop), eq(productCatalog), eq(productId), any());
        verify(basket).getItems();
    }

    @Test
    void testProcessPurchaseThrowsExceptionWhenInsufficientFunds_UNIT() {
        doReturn(BigDecimal.valueOf(100)).when(mockCashierDeskServiceImpl).getTotalBasketPrice(any(), any(), any());
        when(basket.getItems()).thenReturn(new HashMap<>());

        assertThrows(InsufficientFundsException.class, () ->
                mockCashierDeskServiceImpl.processPurchase(shop, productCatalog, basket, BigDecimal.valueOf(5), cashierDesk)
        );

        verify(mockCashierDeskServiceImpl).getTotalBasketPrice(any(), any(), any());
        verify(basket).getItems();
    }

    @Test
    void testProcessPurchaseCreatesReceiptOnSuccess_UNIT() {
        Receipt receipt = new Receipt();

        when(cashierDesk.getCashier()).thenReturn(cashier);
        when(cashier.getName()).thenReturn("John");
        when(basket.getItems()).thenReturn(new HashMap<>());
        when(mockCashierDeskServiceImpl.createReceipt(cashier, shop, productCatalog, basket)).thenReturn(receipt);
        Mockito.doNothing().when(mockCashierDeskServiceImpl).printReceipt(receipt);
        Mockito.doNothing().when(mockCashierDeskServiceImpl).saveReceiptAsText(eq(receipt), anyString());

        assertDoesNotThrow(() ->
                mockCashierDeskServiceImpl.processPurchase(shop, productCatalog, basket, BigDecimal.valueOf(100), cashierDesk)
        );

        verify(cashierDesk, atLeastOnce()).getCashier();
        verify(cashier, atLeastOnce()).getName();
        verify(basket, atLeastOnce()).getItems();
        verify(mockCashierDeskServiceImpl, atLeastOnce()).createReceipt(cashier, shop, productCatalog, basket);
        verify(mockCashierDeskServiceImpl, atLeastOnce()).printReceipt(receipt);
        verify(mockCashierDeskServiceImpl, atLeastOnce()).saveReceiptAsText(eq(receipt), anyString());

    }

    @Test
    void testProcessPurchaseForAllCustomersProcessesEachCustomer_UNIT() {
        Customer customer1 = mock(Customer.class);
        Customer customer2 = mock(Customer.class);
        List<Customer> customers = Arrays.asList(customer1, customer2);

        when(customer1.getBasket()).thenReturn(basket);
        when(customer1.getBudget()).thenReturn(new BigDecimal("50.00"));
        when(customer2.getBasket()).thenReturn(basket);
        when(customer2.getBudget()).thenReturn(new BigDecimal("30.00"));

        doNothing().when(mockCashierDeskServiceImpl).processPurchase(shop, productCatalog, basket, new BigDecimal("50.00"), cashierDesk);
        doNothing().when(mockCashierDeskServiceImpl).processPurchase(shop, productCatalog, basket, new BigDecimal("30.00"), cashierDesk);

        mockCashierDeskServiceImpl.processPurchaseForAllCustomers(shop, productCatalog, customers, cashierDesk);

        verify(mockCashierDeskServiceImpl).processPurchase(shop, productCatalog, basket, new BigDecimal("50.00"), cashierDesk);
        verify(mockCashierDeskServiceImpl).processPurchase(shop, productCatalog, basket, new BigDecimal("30.00"), cashierDesk);
    }

    @Test
    void testGetTotalCustomerSpendingCalculatesCorrectly_UNIT() {
        Customer customer1 = mock(Customer.class);
        Customer customer2 = mock(Customer.class);
        List<Customer> customers = Arrays.asList(customer1, customer2);

        BigDecimal price1 = new BigDecimal("20.50");
        BigDecimal price2 = new BigDecimal("15.75");

        when(customer1.getBasket()).thenReturn(basket);
        when(customer2.getBasket()).thenReturn(basket);
        when(mockCashierDeskServiceImpl.getTotalBasketPrice(shop, productCatalog, basket)).thenReturn(price1, price2);

        BigDecimal totalSpending = mockCashierDeskServiceImpl.getTotalCustomerSpending(shop, productCatalog, customers);

        assertEquals(price1.add(price2), totalSpending);
        verify(customer1).getBasket();
        verify(customer2).getBasket();
        verify(mockCashierDeskServiceImpl, times(2)).getTotalBasketPrice(shop, productCatalog, basket);
    }

    @Test
    void testCreateReceiptReturnsValidReceipt_UNIT() {
        when(cashier.getName()).thenReturn("John");
        when(basket.getItems()).thenReturn(new HashMap<>());

        Receipt receipt = cashierDeskServiceImpl.createReceipt(cashier, shop, productCatalog, basket);
        assertNotNull(receipt);
        assertEquals("John", receipt.getCashierName());
        assertEquals(BigDecimal.ZERO.setScale(2), receipt.getTotal());

        verify(cashier).getName();
        verify(basket, times(2)).getItems();
    }
}