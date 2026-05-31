package tea4life.order_service.order.service;

import tea4life.order_service.dto.request.order.CreateOrderRequest;
import tea4life.order_service.dto.request.order.CheckoutOrderRequest;
import tea4life.order_service.dto.response.order.OrderResponse;
import tea4life.order_service.model.constant.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse checkoutMyCart(CheckoutOrderRequest request);

    List<OrderResponse> getMyOrders(OrderStatus status);

    OrderResponse getMyOrderById(Long orderId);

    OrderResponse cancelMyOrder(Long orderId);
}


