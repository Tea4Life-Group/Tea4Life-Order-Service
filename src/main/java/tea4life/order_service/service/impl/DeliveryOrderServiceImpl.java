package tea4life.order_service.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tea4life.order_service.context.UserContext;
import tea4life.order_service.dto.response.order.DeliveryOrderItemResponse;
import tea4life.order_service.dto.response.order.DeliveryOrderResponse;
import tea4life.order_service.model.constant.OrderStatus;
import tea4life.order_service.model.constant.PaymentMethod;
import tea4life.order_service.model.constant.PaymentStatus;
import tea4life.order_service.model.order.Order;
import tea4life.order_service.model.order.OrderItem;
import tea4life.order_service.model.payment.Payment;
import tea4life.order_service.repository.DriverRepository;
import tea4life.order_service.repository.OrderRepository;
import tea4life.order_service.service.DeliveryOrderService;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    // Repository
    DriverRepository driverRepository;
    OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryOrderResponse> findAvailableOrders() {
        return orderRepository.findByStatusAndDriverKeycloakIdIsNullOrderByCreatedAtDesc(OrderStatus.READY_FOR_DELIVERY).stream()
                .map(this::toDeliveryOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryOrderResponse> findShippingOrders() {
        List<Order> orders = isAdmin()
                ? orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.DELIVERING)
                : orderRepository.findByStatusAndDriverKeycloakIdOrderByCreatedAtDesc(OrderStatus.DELIVERING, resolveCurrentKeycloakId());

        return orders.stream()
                .map(this::toDeliveryOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryOrderResponse findOrderById(Long orderId) {
        Order order = findOrderEntityById(orderId);

        if (!isAdmin() && (order.getStatus() == OrderStatus.DELIVERING || order.getStatus() == OrderStatus.COMPLETED)) {
            ensureCurrentDriverOwnsOrder(order);
        }

        return toDeliveryOrderResponse(order);
    }

    @Override
    public DeliveryOrderResponse pickupOrder(Long orderId) {
        Order order = findOrderEntityById(orderId);
        ensureStatus(order, OrderStatus.READY_FOR_DELIVERY, "Chỉ có thể lấy đơn ở trạng thái READY_FOR_DELIVERY");
        if (order.getDriverKeycloakId() != null && !order.getDriverKeycloakId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng đã được tài xế khác nhận");
        }

        String currentKeycloakId = resolveCurrentKeycloakId();
        order.setDriverKeycloakId(currentKeycloakId);
        order.setStatus(OrderStatus.DELIVERING);
        return toDeliveryOrderResponse(orderRepository.save(order));
    }

    @Override
    public DeliveryOrderResponse completeOrder(Long orderId) {
        Order order = orderRepository.findByIdAndDriverKeycloakId(orderId, resolveCurrentKeycloakId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không được phép hoàn tất đơn này"));
        ensureStatus(order, OrderStatus.DELIVERING, "Chỉ có thể hoàn tất đơn ở trạng thái DELIVERING");
        order.setStatus(OrderStatus.COMPLETED);

        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            Payment payment = order.getPayment();
            if (payment != null) {
                payment.setStatus(PaymentStatus.COMPLETED);
            }
        }

        return toDeliveryOrderResponse(orderRepository.save(order));
    }

    // =================================================
    // Lookup
    // =================================================

    private Order findOrderEntityById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Order với ID: " + orderId));
    }

    // =================================================
    // Validation
    // =================================================

    private String resolveCurrentKeycloakId() {
        UserContext context = UserContext.get();
        if (context == null || context.getKeycloakId() == null || context.getKeycloakId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được người dùng hiện tại");
        }
        String keycloakId = context.getKeycloakId().trim();
        if (driverRepository.findByKeycloakId(keycloakId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải là tài xế hợp lệ");
        }
        return keycloakId;
    }

    private void ensureStatus(Order order, OrderStatus expectedStatus, String message) {
        if (order.getStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private void ensureCurrentDriverOwnsOrder(Order order) {
        String currentKeycloakId = resolveCurrentKeycloakId();
        if (order.getDriverKeycloakId() == null || !order.getDriverKeycloakId().equals(currentKeycloakId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không được phép xem đơn này");
        }
    }

    private boolean isAdmin() {
        UserContext context = UserContext.get();
        return context != null && "ADMIN".equalsIgnoreCase(context.getRole());
    }

    // =================================================
    // Mapping
    // =================================================

    private DeliveryOrderResponse toDeliveryOrderResponse(Order order) {
        List<DeliveryOrderItemResponse> items = order.getOrderItems() == null
                ? List.of()
                : order.getOrderItems().stream()
                .map(this::toDeliveryOrderItemResponse)
                .toList();

        return new DeliveryOrderResponse(
                order.getId() == null ? null : order.getId().toString(),
                order.getOrderCode(),
                order.getReceiverName(),
                order.getPhone(),
                order.getProvince(),
                order.getWard(),
                order.getDetail(),
                order.getStore() == null || order.getStore().getId() == null ? null : order.getStore().getId().toString(),
                order.getStore() == null ? null : order.getStore().getName(),
                order.getStore() == null ? null : order.getStore().getAddress(),
                order.getDriverKeycloakId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getFinalPrice(),
                order.getCreatedAt(),
                items
        );
    }

    private DeliveryOrderItemResponse toDeliveryOrderItemResponse(OrderItem item) {
        return new DeliveryOrderItemResponse(
                item.getId() == null ? null : item.getId().toString(),
                item.getProductId() == null ? null : item.getProductId().toString(),
                item.getProductName(),
                item.getProductImageUrl(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubTotal()
        );
    }
}
