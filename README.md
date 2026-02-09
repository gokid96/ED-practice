# Kafka Saga + Outbox 패턴

## 아키텍처

```text
                         [Kafka]
                        /   |   \
  ┌──────────────┐    /     |     \    ┌──────────────┐
  │ Order Service │─────────────────── │Payment Service│
  │  (Saga 오케)  │   payment-topic    │  결제 처리     │
  │              │←────────────────── │  보상(롤백)    │
  │  [Order DB]  │  order-response    │ [Payment DB] │
  │  [Outbox   ] │                    └──────────────┘
  └──────┬───────┘
         │          inventory-topic   ┌────────────────┐
         └───────────────────────────→│Inventory Service│
                   order-response     │  재고 차감       │
         ←────────────────────────────│ [Inventory DB]  │
                                      └────────────────┘
```

## 패턴별 포인트

### Outbox 패턴 (Order Service)
- `orders` 테이블과 `outbox` 테이블이 **같은 DB 트랜잭션**으로 저장됨
- `OutboxPublisher`가 1초마다 폴링해서 미발행 이벤트를 Kafka로 전송
- Kafka가 죽어도 이벤트가 유실되지 않음 (DB에 남아있으니까)

### Saga 패턴 (전체 흐름)
- **정상**: 주문 → 결제 성공 → 재고 차감 성공 → 완료
- **보상**: 주문 → 결제 성공 → 재고 부족 → **결제 롤백** → 주문 취소

## 테스트 시나리오

| 시나리오 | 상품 | 수량 | 가격 | 예상 결과 |
|---------|------|------|------|----------|
| 성공 | 맥북 | 1 | 500000 | COMPLETED |
| 재고부족→보상 | 아이패드 | 1 | 500000 | CANCELLED (결제 롤백) |
| 결제실패 | 맥북 | 3 | 500000 | CANCELLED (150만원 초과) |

## 실행 순서

```bash
# 1. 인프라
docker compose up -d

# 2. 서비스 (터미널 3개)
cd order-service && ./gradlew bootRun
cd payment-service && ./gradlew bootRun
cd inventory-service && ./gradlew bootRun

# 3. 테스트
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productName":"아이패드","quantity":1,"price":500000}'

# 4. 상태 확인
curl http://localhost:8081/api/orders/1
```
