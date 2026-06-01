package tea4life.order_service.dto.response.order;

import java.math.BigDecimal;
import java.util.List;

public record StoreOrderItemResponse(
        String id,
        String productId,
        List<OrderItemOptionResponse> selectedOptions,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subTotal
) {
}
