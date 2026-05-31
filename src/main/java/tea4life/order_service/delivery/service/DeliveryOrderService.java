package tea4life.order_service.delivery.service;

import tea4life.order_service.dto.response.order.DeliveryOrderResponse;

import java.util.List;

public interface DeliveryOrderService {

    List<DeliveryOrderResponse> findAvailableOrders();

    List<DeliveryOrderResponse> findShippingOrders();

    DeliveryOrderResponse findOrderById(Long orderId);

    DeliveryOrderResponse pickupOrder(Long orderId);

    DeliveryOrderResponse completeOrder(Long orderId);
}


