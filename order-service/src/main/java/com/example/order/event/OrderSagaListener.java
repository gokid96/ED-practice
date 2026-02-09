package com.example.order.event;

import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Saga 오케스트레이터 역할
 *
 * Payment, Inventory 서비스의 응답을 듣고
 * 다음 단계를 결정한다.
 *
 * 성공 → 다음 단계 진행
 * 실패 → 보상 트랜잭션 실행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-response-topic", groupId = "order-group")
    public void handleResponse(String message) {
        try {
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            log.info("📩 응답 수신: type={}, orderId={}", event.getEventType(), event.getOrderId());

            switch (event.getEventType()) {
                case OrderEvent.PAYMENT_SUCCESS:
                    // 결제 성공 → 재고 차감 요청
                    orderService.handlePaymentSuccess(event);
                    break;

                case OrderEvent.PAYMENT_FAILED:
                    // 결제 실패 → 주문 취소
                    orderService.handlePaymentFailed(event);
                    break;

                case OrderEvent.INVENTORY_SUCCESS:
                    // 재고 차감 성공 → 주문 완료!
                    orderService.handleInventorySuccess(event);
                    break;

                case OrderEvent.INVENTORY_FAILED:
                    // ★ 재고 실패 → 결제 롤백 (보상 트랜잭션)
                    orderService.handleInventoryFailed(event);
                    break;

                case OrderEvent.PAYMENT_ROLLBACK + "_DONE":
                    // 보상 완료 → 주문 최종 취소
                    orderService.handleRollbackComplete(event);
                    break;

                default:
                    log.warn("⚠️ 알 수 없는 이벤트 타입: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("❗ 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }
}
