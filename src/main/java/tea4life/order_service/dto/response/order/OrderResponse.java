package tea4life.order_service.dto.response.order;

import tea4life.order_service.model.constant.OrderStatus;
import tea4life.order_service.model.constant.PaymentMethod;
import tea4life.order_service.model.constant.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String orderCode,
        String receiverName,
        String phone,
        String province,
        String ward,
        String detail,
        String storeId,
        String storeName,
        OrderStatus status,
        BigDecimal priceBeforeDiscount,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String note,
        Instant createdAt,
        List<OrderItemResponse> items,

        String checkoutUrl
) {
}
