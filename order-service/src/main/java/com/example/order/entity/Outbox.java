package com.example.order.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 아웃박스 패턴의 핵심!
 *
 * 비즈니스 로직(Order 저장)과 이벤트 발행을 하나의 DB 트랜잭션으로 묶기 위해
 * 이벤트를 Kafka로 직접 보내지 않고, 같은 DB의 outbox 테이블에 먼저 저장한다.
 * 별도 스케줄러가 이 테이블을 폴링해서 Kafka로 발행한다.
 */
@Entity
@Table(name = "outbox")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kafka 토픽 이름 */
    private String topic;

    /** 메시지 키 (주문 ID 등) */
    private String messageKey;

    /** JSON 형태의 이벤트 페이로드 */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /** 발행 여부 */
    private boolean sent;

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.sent = false;
    }
}
