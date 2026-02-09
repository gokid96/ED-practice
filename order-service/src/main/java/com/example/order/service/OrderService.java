package com.example.order.service;

import com.example.order.entity.Order;
import com.example.order.entity.Order.OrderStatus;
import com.example.order.entity.Outbox;
import com.example.order.event.OrderEvent;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * 주문 생성 + Outbox에 결제 요청 이벤트 저장
     *
     * ★ 핵심 포인트: 같은 트랜잭션 안에서 Order 저장 + Outbox 저장
     * → DB 트랜잭션이 커밋되면 둘 다 저장됨
     * → DB 트랜잭션이 롤백되면 둘 다 안 됨
     * → Kafka 발행은 별도 스케줄러가 담당 (OutboxPublisher)
     */
    @Transactional
    public Order createOrder(String productName, int quantity, int price) {
        // 1. 주문 저장
        Order order = Order.builder()
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .status(OrderStatus.PAYMENT_PENDING)
                .build();
        orderRepository.save(order);
        log.info("✅ 주문 생성: orderId={}, product={}", order.getId(), productName);

        // 2. 결제 요청 이벤트를 Outbox 테이블에 저장 (같은 트랜잭션!)
        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .eventType(OrderEvent.PAYMENT_REQUEST)
                .build();
        saveToOutbox("payment-topic", order.getId().toString(), event);

        return order;
    }

    /**
     * Saga 오케스트레이터: 결제 성공 응답 처리
     * → 다음 단계인 재고 차감 요청
     */
    @Transactional
    public void handlePaymentSuccess(OrderEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
        order.setStatus(OrderStatus.INVENTORY_PENDING);
        orderRepository.save(order);
        log.info("✅ 결제 성공 확인: orderId={} → 재고 차감 요청", event.getOrderId());

        // 재고 차감 요청 이벤트를 Outbox에 저장
        OrderEvent inventoryEvent = OrderEvent.builder()
                .orderId(event.getOrderId())
                .productName(event.getProductName())
                .quantity(event.getQuantity())
                .price(event.getPrice())
                .eventType(OrderEvent.INVENTORY_REQUEST)
                .build();
        saveToOutbox("inventory-topic", event.getOrderId().toString(), inventoryEvent);
    }

    /**
     * Saga 오케스트레이터: 재고 차감 성공 → 주문 완료
     */
    @Transactional
    public void handleInventorySuccess(OrderEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
        log.info("🎉 주문 완료! orderId={}", event.getOrderId());
    }

    /**
     * Saga 보상 트랜잭션: 결제 실패 → 주문 취소
     */
    @Transactional
    public void handlePaymentFailed(OrderEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("❌ 결제 실패 → 주문 취소: orderId={}", event.getOrderId());
    }

    /**
     * ★ Saga 보상 트랜잭션: 재고 실패 → 결제 롤백 요청
     *
     * 이게 Saga의 핵심!
     * 재고 차감이 실패했으니, 이미 성공한 결제를 취소(보상)해야 한다.
     */
    @Transactional
    public void handleInventoryFailed(OrderEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
        order.setStatus(OrderStatus.COMPENSATING);
        orderRepository.save(order);
        log.info("⚠️ 재고 부족 → 결제 롤백 요청: orderId={}", event.getOrderId());

        // 결제 취소(보상) 이벤트를 Outbox에 저장
        OrderEvent rollbackEvent = OrderEvent.builder()
                .orderId(event.getOrderId())
                .productName(event.getProductName())
                .quantity(event.getQuantity())
                .price(event.getPrice())
                .eventType(OrderEvent.PAYMENT_ROLLBACK)
                .build();
        saveToOutbox("payment-topic", event.getOrderId().toString(), rollbackEvent);
    }

    /**
     * 보상 트랜잭션 완료 후 주문 최종 취소
     */
    @Transactional
    public void handleRollbackComplete(OrderEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("🔄 결제 롤백 완료 → 주문 최종 취소: orderId={}", event.getOrderId());
    }

    // ──────────────── Outbox 저장 헬퍼 ────────────────

    private void saveToOutbox(String topic, String key, OrderEvent event) {
        try {
            Outbox outbox = Outbox.builder()
                    .topic(topic)
                    .messageKey(key)
                    .payload(objectMapper.writeValueAsString(event))
                    .build();
            outboxRepository.save(outbox);
            log.debug("📤 Outbox 저장: topic={}, key={}", topic, key);
        } catch (Exception e) {
            throw new RuntimeException("Outbox 직렬화 실패", e);
        }
    }
}
