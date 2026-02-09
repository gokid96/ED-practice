package com.example.order.controller;

import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    /**
     * 주문 생성 API
     * POST /api/orders
     * { "productName": "맥북", "quantity": 1, "price": 2000000 }
     */
    @PostMapping
        public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> request) {
        String productName = (String) request.get("productName");
        int quantity = (int) request.get("quantity");
        int price = (int) request.get("price");

        Order order = orderService.createOrder(productName, quantity, price);
        return ResponseEntity.ok(order);
    }

    /** 주문 상태 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 전체 주문 조회 */
    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }
}
