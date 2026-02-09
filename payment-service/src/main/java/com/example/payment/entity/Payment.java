package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private int amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    public enum PaymentStatus {
        COMPLETED,
        ROLLED_BACK
    }
}
