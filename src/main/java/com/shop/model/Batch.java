package com.shop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Batch implements Comparable<Batch> {
    private LocalDate arrivalDate;
    private int quantity;

    @Override
    public int compareTo(Batch other) {
        return this.arrivalDate.compareTo(other.arrivalDate);
    }

    @Override
    public String toString() {
        return "Batch{" +
                "arrivalDate=" + arrivalDate +
                ", quantity=" + quantity +
                '}';
    }
}
