package com.example.order.repository;

import com.example.order.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    /** 아직 발행되지 않은 이벤트 조회 (폴링용) */
    List<Outbox> findBySentFalseOrderByCreatedAtAsc();
}
