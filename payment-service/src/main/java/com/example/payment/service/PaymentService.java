package com.example.payment.service;

import com.example.payment.entity.Payment;
import com.example.payment.entity.Payment.PaymentStatus;
import com.example.payment.event.OrderEvent;
import com.example.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-topic", groupId = "payment-group")
    public void handlePaymentEvent(String message) {
        try {
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            log.info("📩 결제 이벤트 수신: type={}, orderId={}", event.getEventType(), event.getOrderId());

            switch (event.getEventType()) {
                case OrderEvent.PAYMENT_REQUEST:
                    processPayment(event);
                    break;

                case OrderEvent.PAYMENT_ROLLBACK:
                    // ★ 보상 트랜잭션: 재고 실패로 인한 결제 취소
                    rollbackPayment(event);
                    break;

                default:
                    log.warn("알 수 없는 이벤트: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("❗ 결제 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 결제 처리
     * 실습을 위해 금액이 100만원 초과면 실패하도록 설정
     */
    @Transactional
    public void processPayment(OrderEvent event) {
        try {
            // 결제 실패 시뮬레이션: 100만원 초과 시 실패
            if (event.getPrice() * event.getQuantity() > 1_000_000) {
                log.info("💸 결제 실패 (금액 초과): orderId={}, 총액={}",
                        event.getOrderId(), event.getPrice() * event.getQuantity());
                sendResponse(event, OrderEvent.PAYMENT_FAILED);
                return;
            }

            // 결제 성공
            Payment payment = Payment.builder()
                    .orderId(event.getOrderId())
                    .amount(event.getPrice() * event.getQuantity())
                    .status(PaymentStatus.COMPLETED)
                    .build();
            paymentRepository.save(payment);

            log.info("✅ 결제 성공: orderId={}, 금액={}", event.getOrderId(), payment.getAmount());
            sendResponse(event, OrderEvent.PAYMENT_SUCCESS);

        } catch (Exception e) {
            log.error("❗ 결제 처리 중 오류: {}", e.getMessage());
            sendResponse(event, OrderEvent.PAYMENT_FAILED);
        }
    }

    /**
     * ★ 보상 트랜잭션 (Saga의 핵심!)
     *
     * 재고 차감이 실패했기 때문에, 이미 완료된 결제를 취소한다.
     * 실제로는 PG사 환불 API 호출 등이 들어갈 자리.
     */
    @Transactional
    public void rollbackPayment(OrderEvent event) {
        paymentRepository.findByOrderId(event.getOrderId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.ROLLED_BACK);
            paymentRepository.save(payment);
            log.info("🔄 결제 롤백 완료: orderId={}, 금액={}", event.getOrderId(), payment.getAmount());
        });

        // 롤백 완료 알림
        sendResponse(event, OrderEvent.PAYMENT_ROLLBACK_DONE);
    }

    private void sendResponse(OrderEvent event, String eventType) {
        try {
            OrderEvent response = OrderEvent.builder()
                    .orderId(event.getOrderId())
                    .productName(event.getProductName())
                    .quantity(event.getQuantity())
                    .price(event.getPrice())
                    .eventType(eventType)
                    .build();
            kafkaTemplate.send("order-response-topic",
                    event.getOrderId().toString(),
                    objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("❗ 응답 발행 실패: {}", e.getMessage());
        }
    }
}
