package com.example.inventory.config;

import com.example.inventory.entity.Inventory;
import com.example.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 테스트용 재고 데이터 삽입
 *
 * 맥북: 5개 → 주문 가능
 * 아이패드: 0개 → 주문하면 재고 부족 → Saga 보상 트랜잭션 발동!
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    @Override
    public void run(String... args) {
        inventoryRepository.save(Inventory.builder()
                .productName("맥북")
                .stock(5)
                .build());

        inventoryRepository.save(Inventory.builder()
                .productName("아이패드")
                .stock(0)  // ← 재고 없음! Saga 테스트용
                .build());

        log.info("📦 초기 재고 데이터 삽입 완료: 맥북(5개), 아이패드(0개)");
    }
}
