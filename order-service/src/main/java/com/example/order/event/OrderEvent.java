package com.example.order.event;

import lombok.*;
import java.io.Serializable;

/**
 * 서비스 간 주고받는 이벤트 메시지
 */
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

    /**
     * 이벤트 타입 상수
     *
     * [정상 흐름]
     * PAYMENT_REQUEST  → Order → Payment: "결제해줘"
     * PAYMENT_SUCCESS  → Payment → Order: "결제 성공했어"
     * INVENTORY_REQUEST → Order → Inventory: "재고 차감해줘"
     * INVENTORY_SUCCESS → Inventory → Order: "재고 차감 성공"
     *
     * [실패 + 보상 흐름 (Saga)]
     * PAYMENT_FAILED    → Payment → Order: "결제 실패했어"
     * INVENTORY_FAILED  → Inventory → Order: "재고 부족이야"
     * PAYMENT_ROLLBACK  → Order → Payment: "결제 취소해줘" (보상 트랜잭션)
     */
    public static final String PAYMENT_REQUEST = "PAYMENT_REQUEST";
    public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String INVENTORY_REQUEST = "INVENTORY_REQUEST";
    public static final String INVENTORY_SUCCESS = "INVENTORY_SUCCESS";
    public static final String INVENTORY_FAILED = "INVENTORY_FAILED";
    public static final String PAYMENT_ROLLBACK = "PAYMENT_ROLLBACK";
}
