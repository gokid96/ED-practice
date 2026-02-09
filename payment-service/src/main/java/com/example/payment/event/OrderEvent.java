package com.example.payment.event;

import lombok.*;
import java.io.Serializable;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString
public class OrderEvent implements Serializable {
    private Long orderId;
    private String productName;
    private int quantity;
    private int price;
    private String eventType;

    public static final String PAYMENT_REQUEST = "PAYMENT_REQUEST";
    public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_ROLLBACK = "PAYMENT_ROLLBACK";
    public static final String PAYMENT_ROLLBACK_DONE = "PAYMENT_ROLLBACK_DONE";
}
