package com.example.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    /** Order → Payment 으로 보내는 토픽 */
    @Bean
    public NewTopic paymentTopic() {
        return new NewTopic("payment-topic", 1, (short) 1);
    }

    /** Order → Inventory 로 보내는 토픽 */
    @Bean
    public NewTopic inventoryTopic() {
        return new NewTopic("inventory-topic", 1, (short) 1);
    }

    /** Payment/Inventory → Order 로 응답하는 토픽 */
    @Bean
    public NewTopic orderResponseTopic() {
        return new NewTopic("order-response-topic", 1, (short) 1);
    }
}
