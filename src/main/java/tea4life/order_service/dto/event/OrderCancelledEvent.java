package tea4life.order_service.dto.event;

import java.util.List;

public record OrderCancelledEvent(
        String orderId,
        String userKeycloakId,
        List<OrderPlacedItemEvent> items
) {
}
