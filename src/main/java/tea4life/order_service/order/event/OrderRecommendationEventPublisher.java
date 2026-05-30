package tea4life.order_service.order.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tea4life.order_service.dto.event.OrderCancelledEvent;
import tea4life.order_service.dto.event.OrderPlacedEvent;
import tea4life.order_service.dto.event.OrderPlacedItemEvent;
import tea4life.order_service.model.order.Order;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderRecommendationEventPublisher {

    ObjectMapper objectMapper;
    KafkaTemplate<String, String> kafkaTemplate;

    @NonFinal
    @Value("${spring.kafka.topic.order-placed}")
    String orderPlacedTopic;

    @NonFinal
    @Value("${spring.kafka.topic.order-cancelled}")
    String orderCancelledTopic;

    public void publishOrderPlacedAfterCommit(Order order) {
        OrderPlacedEvent event = toOrderPlacedEvent(order);
        if (event.items().isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishOrderPlaced(event);
                }
            });
            return;
        }

        publishOrderPlaced(event);
    }

    public void publishOrderCancelledAfterCommit(Order order) {
        OrderCancelledEvent event = toOrderCancelledEvent(order);
        if (event.items().isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishOrderCancelled(event);
                }
            });
            return;
        }

        publishOrderCancelled(event);
    }

    private void publishOrderPlaced(OrderPlacedEvent event) {
        try {
            kafkaTemplate.send(orderPlacedTopic, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize order_placed event for orderId={}: {}", event.orderId(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("Failed to publish order_placed event for orderId={}: {}", event.orderId(), ex.getMessage());
        }
    }

    private void publishOrderCancelled(OrderCancelledEvent event) {
        try {
            kafkaTemplate.send(orderCancelledTopic, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize order_cancelled event for orderId={}: {}", event.orderId(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("Failed to publish order_cancelled event for orderId={}: {}", event.orderId(), ex.getMessage());
        }
    }

    private OrderPlacedEvent toOrderPlacedEvent(Order order) {
        List<OrderPlacedItemEvent> items = order.getOrderItems() == null
                ? List.of()
                : order.getOrderItems().stream()
                .filter(item -> item.getProductId() != null)
                .map(item -> new OrderPlacedItemEvent(
                        item.getProductId(),
                        null,
                        item.getQuantity(),
                        extractOptionValueIds(item.getSelectedOptionsSnapshot())
                ))
                .toList();

        return new OrderPlacedEvent(
                order.getId() == null ? null : order.getId().toString(),
                order.getKeycloakId(),
                items
        );
    }

    private OrderCancelledEvent toOrderCancelledEvent(Order order) {
        OrderPlacedEvent placedEvent = toOrderPlacedEvent(order);
        return new OrderCancelledEvent(
                placedEvent.orderId(),
                placedEvent.userKeycloakId(),
                placedEvent.items()
        );
    }

    private List<Long> extractOptionValueIds(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(snapshot);
            if (!root.isArray()) {
                return List.of();
            }

            List<Long> optionValueIds = new ArrayList<>();
            for (JsonNode optionNode : root) {
                Long optionValueId = parseLongOrNull(optionNode.path("productOptionValueId").asText(null));
                if (optionValueId != null) {
                    optionValueIds.add(optionValueId);
                }
            }

            return optionValueIds.stream().distinct().toList();
        } catch (JsonProcessingException ex) {
            log.warn("Cannot parse selectedOptions snapshot for recommendation event: {}", ex.getMessage());
            return List.of();
        }
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}


