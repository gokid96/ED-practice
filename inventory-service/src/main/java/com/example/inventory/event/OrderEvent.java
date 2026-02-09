package com.example.inventory.event;

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

    public static final String INVENTORY_REQUEST = "INVENTORY_REQUEST";
    public static final String INVENTORY_SUCCESS = "INVENTORY_SUCCESS";
    public static final String INVENTORY_FAILED = "INVENTORY_FAILED";
}
