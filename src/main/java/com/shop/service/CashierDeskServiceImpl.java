package com.shop.service;

import com.shop.exception.InsufficientFundsException;
import com.shop.model.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CashierDeskServiceImpl implements CashierDeskService {
    private final ShopServiceImpl shopServiceImpl;

    public CashierDeskServiceImpl(ShopServiceImpl shopServiceImpl) {
        this.shopServiceImpl = shopServiceImpl;
    }

    @Override
    public BigDecimal getTotalBasketPrice(Shop shop, ProductCatalog productCatalog, Basket basket) {
        return basket.getItems().entrySet().stream()
                .map(entry -> {
                    String productId = entry.getKey();
                    PriorityQueue<Batch> batches = entry.getValue();

                    Product product = productCatalog.getProductCatalog().get(productId);

                    BigDecimal totalProductPrice = BigDecimal.ZERO;

                    PriorityQueue<Batch> batchesCopy = new PriorityQueue<>(batches);

                    while (!batchesCopy.isEmpty()) {
                        Batch batch = batchesCopy.poll();

                        LocalDate expirationDate;
                        if (product instanceof PerishableProduct) {
                            PerishableProduct perishableProduct = (PerishableProduct) product;
                            expirationDate = batch.getArrivalDate().plusDays(perishableProduct.getExpirationDays());
                        } else {
                            expirationDate = DateWrapper.currentDate();
                        }

                        BigDecimal priceForDate = shopServiceImpl.getProductPrice(shop, productCatalog, productId, expirationDate);

                        BigDecimal batchPrice = priceForDate.multiply(BigDecimal.valueOf(batch.getQuantity()));
                        totalProductPrice = totalProductPrice.add(batchPrice);
                    }

                    return totalProductPrice;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalCustomerSpending(Shop shop, ProductCatalog productCatalog, List<Customer> customers) {
        BigDecimal income = BigDecimal.ZERO;
        for (Customer customer : customers) {
            income = income.add(getTotalBasketPrice(shop, productCatalog, customer.getBasket()));
        }
        return income;
    }

    @Override
    public void processPurchase(Shop shop, ProductCatalog productCatalog, Basket basket, BigDecimal budget, CashierDesk cashierDesk) {
        BigDecimal totalCost = getTotalBasketPrice(shop, productCatalog, basket);

        if (totalCost.compareTo(budget) > 0) {
            basket.getItems().forEach((productId, batches) -> {
                PriorityQueue<Batch> batchesCopy = new PriorityQueue<>(batches);

                while (!batchesCopy.isEmpty()) {
                    Batch batch = batchesCopy.poll();
                    shopServiceImpl.addProduct(shop, productId, batch.getQuantity(), batch.getArrivalDate());
                }
            });

            throw new InsufficientFundsException("Customer budget of " + budget + " is insufficient for total cost: " + totalCost);
        }

        Receipt receipt = createReceipt(cashierDesk.getCashier(), shop, productCatalog, basket);

        printReceipt(receipt);
        saveReceiptAsText(receipt, cashierDesk.getCashier().getName());
    }

    @Override
    public void processPurchaseForAllCustomers(Shop shop, ProductCatalog productCatalog, List<Customer> customers, CashierDesk cashierDesk) {
        for (Customer customer : customers) {
            processPurchase(shop, productCatalog, customer.getBasket(), customer.getBudget(), cashierDesk);
        }
    }
    @Override
    public Receipt createReceipt(Cashier cashier, Shop shop, ProductCatalog productCatalog, Basket basket) {
        String id = UUID.randomUUID().toString();
        LocalDate date = DateWrapper.currentDate();

        Map<String, Integer> items = new HashMap<>();
        basket.getItems().forEach((productId, batches) -> {
            int totalQuantity = batches.stream()
                    .mapToInt(Batch::getQuantity)
                    .sum();
            items.put(productId, totalQuantity);
        });

        BigDecimal totalCost = getTotalBasketPrice(shop, productCatalog, basket);

        return new Receipt(id, cashier.getName(), date, items, totalCost);
    }

    @Override
    public void printReceipt(Receipt receipt) {
        try {
            Files.createDirectories(Paths.get("receipts"));

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    Files.newOutputStream(Paths.get("receipts/" + receipt.getId() + ".ser")))) {
                oos.writeObject(receipt);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error printing receipt: " + e.getMessage());
        }
    }

    @Override
    public void saveReceiptAsText(Receipt receipt, String cashierName) {
        try {
            Files.createDirectories(Paths.get("receipts"));

            String fileName = "receipts/" + cashierName + "_" + receipt.getId().substring(0, 4) + "_" +
                    receipt.getDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".txt";

            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
                writer.write("Receipt\n");
                writer.write("========\n");
                writer.write("Cashier: " + receipt.getCashierName() + "\n");
                writer.write("Date: " + receipt.getDate() + "\n");
                writer.write("Items:\n");

                receipt.getItems().forEach((productId, quantity) -> {
                    try {
                        Product product = new ProductCatalog().getProductCatalog().get(productId);
                        String productName = product != null ? product.getName() : productId;
                        BigDecimal price = product != null ? product.getPrice() : BigDecimal.ZERO;

                        writer.write("- " + productName + " x " + quantity + " @ " + price + " each\n");
                    } catch (IOException e) {
                        throw new RuntimeException("Error writing receipt item: " + e.getMessage(), e);
                    }
                });

                writer.write("\nTotal: $" + receipt.getTotal() + "\n");
                writer.write("========\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error saving receipt as text: " + e.getMessage());
        }
    }

    @Override
    public Receipt loadReceipt(String receiptId) {
        try (ObjectInputStream ois = new ObjectInputStream(
                Files.newInputStream(Paths.get("receipts/" + receiptId + ".ser")))) {
            return (Receipt) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error loading receipt: " + e.getMessage());
        }
    }
}