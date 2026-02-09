package com.example.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private int quantity;
    private int price;

    /**
     * Saga 상태 추적
     * CREATED → PAYMENT_PENDING → INVENTORY_PENDING → COMPLETED
     *                ↓                    ↓
     *          PAYMENT_FAILED      COMPENSATING → CANCELLED
     */
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public enum OrderStatus {
        CREATED,
        PAYMENT_PENDING,
        PAYMENT_COMPLETED,
        INVENTORY_PENDING,
        COMPLETED,
        PAYMENT_FAILED,
        INVENTORY_FAILED,
        COMPENSATING,
        CANCELLED
    }
}
