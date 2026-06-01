package tea4life.order_service.store.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tea4life.order_service.context.UserContext;
import tea4life.order_service.dto.response.order.OrderItemOptionResponse;
import tea4life.order_service.dto.response.order.StoreOrderItemResponse;
import tea4life.order_service.dto.response.order.StoreOrderResponse;
import tea4life.order_service.model.constant.OrderStatus;
import tea4life.order_service.model.constant.PaymentMethod;
import tea4life.order_service.model.constant.PaymentStatus;
import tea4life.order_service.model.order.Order;
import tea4life.order_service.model.order.OrderItem;
import tea4life.order_service.model.payment.Payment;
import tea4life.order_service.model.store.StoreEmployee;
import tea4life.order_service.order.event.OrderRecommendationEventPublisher;
import tea4life.order_service.order.policy.OrderStatusPolicy;
import tea4life.order_service.order.repository.OrderRepository;
import tea4life.order_service.store.repository.StoreEmployeeRepository;
import tea4life.order_service.store.service.StoreOrderService;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class StoreOrderServiceImpl implements StoreOrderService {

    // Repository
    OrderRepository orderRepository;
    StoreEmployeeRepository storeEmployeeRepository;
    OrderRecommendationEventPublisher orderRecommendationEventPublisher;
    OrderStatusPolicy orderStatusPolicy;
    ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<StoreOrderResponse> findStoreOrders(Long storeId, OrderStatus status) {
        ensureCurrentUserBelongsToStore(storeId);
        List<Order> orders = (status == null)
                ? orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
                : orderRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, status);

        return orders.stream()
                .map(this::toStoreOrderResponse)
                .toList();
    }

    @Override
    public StoreOrderResponse acceptOrder(Long storeId, Long orderId) {
        Order order = findOrderInStore(storeId, orderId);
        ensureStatus(order, OrderStatus.PENDING, "Chỉ có thể nhận đơn đang ở trạng thái PENDING");
        order.setStatus(OrderStatus.PREPARING);
        return toStoreOrderResponse(orderRepository.save(order));
    }

    @Override
    public StoreOrderResponse markOrderReadyForDelivery(Long storeId, Long orderId) {
        Order order = findOrderInStore(storeId, orderId);
        ensureStatus(order, OrderStatus.PREPARING, "Chỉ có thể xác nhận xong đơn đang ở trạng thái PREPARING");
        order.setStatus(OrderStatus.READY_FOR_DELIVERY);
        return toStoreOrderResponse(orderRepository.save(order));
    }

    @Override
    public StoreOrderResponse cancelOrder(Long storeId, Long orderId) {
        Order order = findOrderInStore(storeId, orderId);
        orderStatusPolicy.ensureStoreCanCancel(order);

        order.setStatus(OrderStatus.CANCELLED);

        if (order.getPaymentMethod() == PaymentMethod.BANKING) {
            order.setPaymentStatus(PaymentStatus.CANCELED);
            Payment payment = order.getPayment();
            if (payment != null) {
                payment.setStatus(PaymentStatus.CANCELED);
            }
        }

        Order savedOrder = orderRepository.save(order);
        orderRecommendationEventPublisher.publishOrderCancelledAfterCommit(savedOrder);
        return toStoreOrderResponse(savedOrder);
    }

    // =================================================
    // Lookup
    // =================================================

    private Order findOrderInStore(Long storeId, Long orderId) {
        ensureCurrentUserBelongsToStore(storeId);
        return orderRepository.findByIdAndStoreId(orderId, storeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng thuộc chi nhánh hiện tại"
                ));
    }

    private void ensureCurrentUserBelongsToStore(Long storeId) {
        if (isAdmin()) {
            return;
        }

        String keycloakId = resolveCurrentKeycloakId();
        if (storeEmployeeRepository.findByStoreIdAndKeycloakId(storeId, keycloakId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không thuộc chi nhánh này");
        }
    }

    // =================================================
    // Validation
    // =================================================

    private String resolveCurrentKeycloakId() {
        UserContext context = UserContext.get();
        if (context == null || context.getKeycloakId() == null || context.getKeycloakId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được người dùng hiện tại");
        }
        return context.getKeycloakId().trim();
    }

    private boolean isAdmin() {
        UserContext context = UserContext.get();
        return context != null && "ADMIN".equalsIgnoreCase(context.getRole());
    }

    private void ensureStatus(Order order, OrderStatus expectedStatus, String message) {
        if (order.getStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    // =================================================
    // Mapping
    // =================================================

    private StoreOrderResponse toStoreOrderResponse(Order order) {
        List<StoreOrderItemResponse> items = order.getOrderItems() == null
                ? List.of()
                : order.getOrderItems().stream()
                .map(this::toStoreOrderItemResponse)
                .toList();

        return new StoreOrderResponse(
                order.getId() == null ? null : order.getId().toString(),
                order.getOrderCode(),
                order.getStore() == null || order.getStore().getId() == null ? null : order.getStore().getId().toString(),
                order.getReceiverName(),
                order.getPhone(),
                order.getProvince(),
                order.getWard(),
                order.getDetail(),
                order.getKeycloakId(),
                order.getStatus(),
                order.getNote(),
                order.getPriceBeforeDiscount(),
                order.getFinalPrice(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getCreatedAt(),
                items
        );
    }

    private StoreOrderItemResponse toStoreOrderItemResponse(OrderItem item) {
        return new StoreOrderItemResponse(
                item.getId() == null ? null : item.getId().toString(),
                item.getProductId() == null ? null : item.getProductId().toString(),
                fromSelectedOptionsSnapshot(item.getSelectedOptionsSnapshot()),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubTotal()
        );
    }

    private List<OrderItemOptionResponse> fromSelectedOptionsSnapshot(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(snapshot, new TypeReference<List<OrderItemOptionResponse>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không đọc được selectedOptions snapshot", ex);
        }
    }
}



