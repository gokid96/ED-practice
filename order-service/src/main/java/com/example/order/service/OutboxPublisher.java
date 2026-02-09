package com.example.order.service;

import com.example.order.entity.Outbox;
import com.example.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 아웃박스 패턴의 핵심 컴포넌트!
 *
 * 주기적으로 outbox 테이블을 확인해서
 * 아직 발행되지 않은 이벤트를 Kafka로 보낸다.
 *
 * [왜 이렇게 하나?]
 * - 비즈니스 로직에서 직접 Kafka에 보내면, DB는 저장됐는데 Kafka 발행이 실패할 수 있음
 * - Outbox 테이블에 저장하면 DB 트랜잭션으로 보장됨
 * - 이 Publisher가 실패해도 다음 폴링 때 다시 시도 → 결국 발행됨 (at-least-once)
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /** 1초마다 미발행 이벤트 확인 후 Kafka로 발행 */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishOutboxMessages() {
        List<Outbox> pendingMessages = outboxRepository.findBySentFalseOrderByCreatedAtAsc();

        for (Outbox outbox : pendingMessages) {
            try {
                // Kafka로 발행
                kafkaTemplate.send(outbox.getTopic(), outbox.getMessageKey(), outbox.getPayload());

                // 발행 성공 표시
                outbox.setSent(true);
                outbox.setSentAt(LocalDateTime.now());
                outboxRepository.save(outbox);

                log.info("📨 Outbox → Kafka 발행 완료: topic={}, key={}",
                        outbox.getTopic(), outbox.getMessageKey());
            } catch (Exception e) {
                // 실패해도 다음 폴링 때 다시 시도됨! (아웃박스의 핵심)
                log.error("❗ Kafka 발행 실패, 다음에 재시도: topic={}, error={}",
                        outbox.getTopic(), e.getMessage());
            }
        }
    }
}
