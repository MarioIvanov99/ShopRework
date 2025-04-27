package com.shop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Receipt implements Serializable {
    private static final long serialVersionUID = 67686234738156688L;
    private String id;
    private String cashierName;
    private LocalDate date;
    private Map<String, Integer> items;
    private BigDecimal total;
}
