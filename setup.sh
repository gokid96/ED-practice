#!/bin/bash
echo "======================================"
echo "  Kafka Saga + Outbox 셋업"
echo "======================================"

# Gradle Wrapper 생성 (각 서비스별)
for service in order-service payment-service inventory-service; do
    echo ""
    echo "$service Gradle Wrapper 생성 중.."
    cd $service
    gradle wrapper --gradle-version 8.10
    chmod +x gradlew
    cd ..
    echo " $service 완료"
done

echo ""
echo "======================================"
echo "  셋업 완료 아래 순서로 실행"
echo "======================================"
echo ""
echo "1  인프라 띄우기 (Kafka + DB 3개):"
echo "   docker compose up -d"
echo ""
echo "2  Kafka UI 확인 (브라우저):"
echo "   http://localhost:8989"
echo ""
echo "3  각 서비스 실행 (터미널 3개):"
echo "   cd order-service && ./gradlew bootRun"
echo "   cd payment-service && ./gradlew bootRun"
echo "   cd inventory-service && ./gradlew bootRun"
echo ""
echo "4  테스트 (다른 터미널에서):"
echo ""
echo "   [성공 케이스] 맥북 1개 주문 (재고 있음, 100만원 이하):"
echo '   curl -X POST http://localhost:8081/api/orders \
     -H "Content-Type: application/json" \
     -d '"'"'{"productName":"맥북","quantity":1,"price":500000}'"'"
echo ""
echo "   [Saga 보상 케이스] 아이패드 주문 (재고 없음 → 결제 롤백):"
echo '   curl -X POST http://localhost:8081/api/orders \
     -H "Content-Type: application/json" \
     -d '"'"'{"productName":"아이패드","quantity":1,"price":500000}'"'"
echo ""
echo "   [결제 실패 케이스] 100만원 초과 주문:"
echo '   curl -X POST http://localhost:8081/api/orders \
     -H "Content-Type: application/json" \
     -d '"'"'{"productName":"맥북","quantity":3,"price":500000}'"'"
echo ""
echo "   주문 상태 확인:"
echo "   curl http://localhost:8081/api/orders"
echo ""
