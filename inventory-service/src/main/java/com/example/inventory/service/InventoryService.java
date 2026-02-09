package com.example.inventory.service;

import com.example.inventory.entity.Inventory;
import com.example.inventory.event.OrderEvent;
import com.example.inventory.repository.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory-topic", groupId = "inventory-group")
    public void handleInventoryEvent(String message) {
        try {
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            log.info("📩 재고 이벤트 수신: orderId={}, product={}, qty={}",
                    event.getOrderId(), event.getProductName(), event.getQuantity());

            deductStock(event);
        } catch (Exception e) {
            log.error("❗ 재고 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 시도
     *
     * 재고 충분 → 차감 후 SUCCESS 응답
     * 재고 부족 → FAILED 응답 → Order Service가 보상 트랜잭션 시작
     */
    @Transactional
    public void deductStock(OrderEvent event) {
        Optional<Inventory> optInventory = inventoryRepository.findByProductName(event.getProductName());

        if (optInventory.isEmpty()) {
            log.info("❌ 상품 없음: {}", event.getProductName());
            sendResponse(event, OrderEvent.INVENTORY_FAILED);
            return;
        }

        Inventory inventory = optInventory.get();

        if (inventory.getStock() < event.getQuantity()) {
            // ★ 재고 부족! → 이 응답이 Saga 보상 트랜잭션을 트리거함
            log.info("❌ 재고 부족: product={}, 현재={}, 요청={}",
                    event.getProductName(), inventory.getStock(), event.getQuantity());
            sendResponse(event, OrderEvent.INVENTORY_FAILED);
            return;
        }

        // 재고 차감 성공
        inventory.setStock(inventory.getStock() - event.getQuantity());
        inventoryRepository.save(inventory);
        log.info("✅ 재고 차감 성공: product={}, 남은재고={}",
                event.getProductName(), inventory.getStock());
        sendResponse(event, OrderEvent.INVENTORY_SUCCESS);
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
