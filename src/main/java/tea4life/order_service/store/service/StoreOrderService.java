package tea4life.order_service.store.service;

import tea4life.order_service.model.constant.OrderStatus;
import tea4life.order_service.dto.response.order.StoreOrderResponse;

import java.util.List;

public interface StoreOrderService {

    List<StoreOrderResponse> findStoreOrders(Long storeId, OrderStatus status);

    StoreOrderResponse acceptOrder(Long storeId, Long orderId);

    StoreOrderResponse markOrderReadyForDelivery(Long storeId, Long orderId);

    StoreOrderResponse cancelOrder(Long storeId, Long orderId);
}


