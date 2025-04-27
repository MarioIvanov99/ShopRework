package com.shop.service;

import com.shop.exception.InsufficientFundsException;
import com.shop.model.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public interface CashierDeskService {
    BigDecimal getTotalBasketPrice(Shop shop, ProductCatalog productCatalog, Basket basket);

    void processPurchase(Shop shop, ProductCatalog productCatalog, Basket basket, BigDecimal budget, CashierDesk cashierDesk);

    void processPurchaseForAllCustomers(Shop shop, ProductCatalog productCatalog, List<Customer> customers, CashierDesk cashierDesk);

    Receipt createReceipt(Cashier cashier, Shop shop, ProductCatalog productCatalog, Basket basket);

    void printReceipt(Receipt receipt);

    void saveReceiptAsText(Receipt receipt, String cashierName);

    Receipt loadReceipt(String receiptId);
}
