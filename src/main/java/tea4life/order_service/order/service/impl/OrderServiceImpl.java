package tea4life.order_service.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tea4life.order_service.context.UserContext;
import tea4life.order_service.dto.request.order.CheckoutOrderRequest;
import tea4life.order_service.dto.request.order.CreateOrderRequest;
import tea4life.order_service.dto.request.order.OrderItemOptionRequest;
import tea4life.order_service.dto.request.order.OrderItemRequest;
import tea4life.order_service.dto.response.order.OrderItemOptionResponse;
import tea4life.order_service.dto.response.order.OrderItemResponse;
import tea4life.order_service.dto.response.order.OrderResponse;
import tea4life.order_service.model.cart.Cart;
import tea4life.order_service.model.cart.CartItem;
import tea4life.order_service.model.constant.OrderStatus;
import tea4life.order_service.model.constant.PaymentMethod;
import tea4life.order_service.model.constant.PaymentStatus;
import tea4life.order_service.model.order.Order;
import tea4life.order_service.model.order.OrderItem;
import tea4life.order_service.model.payment.Payment;
import tea4life.order_service.model.store.Store;
import tea4life.order_service.cart.repository.CartRepository;
import tea4life.order_service.order.event.OrderRecommendationEventPublisher;
import tea4life.order_service.order.policy.OrderStatusPolicy;
import tea4life.order_service.order.repository.OrderRepository;
import tea4life.order_service.store.repository.StoreRepository;
import tea4life.order_service.order.service.OrderService;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    // Repository
    CartRepository cartRepository;
    OrderRepository orderRepository;
    StoreRepository storeRepository;
    OrderRecommendationEventPublisher orderRecommendationEventPublisher;
    OrderStatusPolicy orderStatusPolicy;

    // Mapper / Serializer
    ObjectMapper objectMapper;

    // Payment
    PayOS payOS;

    // Domain frontend
    @NonFinal
    @Value("${app.frontend.url}")
    String frontendDomain;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        String currentKeycloakId = resolveCurrentKeycloakId();
        Store assignedStore = resolveAssignedStore(request.latitude(), request.longitude());

        return persistOrder(
                currentKeycloakId,
                assignedStore,
                request.receiverName(),
                request.phone(),
                request.province(),
                request.ward(),
                request.detail(),
                request.paymentMethod(),
                buildOrderItemsFromRequest(null, request.items()),
                request.shippingFee()
        );
    }

    @Override
    public OrderResponse checkoutMyCart(CheckoutOrderRequest request) {
        String currentKeycloakId = resolveCurrentKeycloakId();
        Cart cart = cartRepository.findByKeycloakId(currentKeycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giỏ hàng hiện tại"));

        List<CartItem> cartItems = cart.getCartItems() == null ? List.of() : cart.getCartItems().stream().toList();
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng đang trống");
        }

        OrderResponse response = persistOrder(
                currentKeycloakId,
                resolveAssignedStore(request.latitude(), request.longitude()),
                request.receiverName(),
                request.phone(),
                request.province(),
                request.ward(),
                request.detail(),
                request.paymentMethod(),
                buildOrderItemsFromCart(null, cartItems),
                request.shippingFee()
        );

        cart.getCartItems().clear();
        cartRepository.save(cart);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(OrderStatus status) {
        String keycloakId = resolveCurrentKeycloakId();
        List<Order> orders = status == null
                ? orderRepository.findByKeycloakIdOrderByCreatedAtDesc(keycloakId)
                : orderRepository.findByKeycloakIdAndStatusOrderByCreatedAtDesc(keycloakId, status);

        return orders.stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(Long orderId) {
        return orderRepository.findByIdAndKeycloakId(orderId, resolveCurrentKeycloakId())
                .map(this::toOrderResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Order với ID: " + orderId));
    }

    @Override
    public OrderResponse cancelMyOrder(Long orderId) {
        Order order = orderRepository.findByIdAndKeycloakId(orderId, resolveCurrentKeycloakId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Order với ID: " + orderId));

        orderStatusPolicy.ensureCustomerCanCancel(order);
        order.setStatus(OrderStatus.CANCELLED);

        if (order.getPaymentMethod() == PaymentMethod.BANKING) {
            order.setPaymentStatus(PaymentStatus.CANCELED);
            if (order.getPayment() != null) {
                order.getPayment().setStatus(PaymentStatus.CANCELED);
            }
        }

        Order savedOrder = orderRepository.save(order);
        orderRecommendationEventPublisher.publishOrderCancelledAfterCommit(savedOrder);
        return toOrderResponse(savedOrder);
    }

    // =================================================
    // Build Order Items
    // =================================================

    private Set<OrderItem> buildOrderItemsFromRequest(Order order, List<OrderItemRequest> itemRequests) {
        Set<OrderItem> items = new LinkedHashSet<>();

        for (OrderItemRequest itemRequest : itemRequests) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(parseProductId(itemRequest.productId()));
            orderItem.setProductName(itemRequest.productName().trim());
            orderItem.setProductImageUrl(trimToNull(itemRequest.productImageUrl()));
            orderItem.setSelectedOptionsSnapshot(toSelectedOptionsSnapshot(itemRequest.selectedOptions()));
            orderItem.setUnitPrice(itemRequest.unitPrice());
            orderItem.setQuantity(itemRequest.quantity());
            orderItem.setSubTotal(itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
            items.add(orderItem);
        }

        return items;
    }

    private Set<OrderItem> buildOrderItemsFromCart(Order order, List<CartItem> cartItems) {
        Set<OrderItem> items = new LinkedHashSet<>();

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(cartItem.getProductName());
            orderItem.setProductImageUrl(cartItem.getProductImageUrl());
            orderItem.setSelectedOptionsSnapshot(cartItem.getSelectedOptionsSnapshot());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubTotal(cartItem.getSubTotal());
            items.add(orderItem);
        }

        return items;
    }

    // =================================================
    // Persist Order
    // =================================================

    private OrderResponse persistOrder(
            String currentKeycloakId,
            Store assignedStore,
            String receiverName,
            String phone,
            String province,
            String ward,
            String detail,
            tea4life.order_service.model.constant.PaymentMethod paymentMethod,
            Set<OrderItem> orderItems,
            BigDecimal shippingFee
    ) {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setKeycloakId(currentKeycloakId);
        order.setStore(assignedStore);
        order.setReceiverName(receiverName.trim());
        order.setPhone(phone.trim());
        order.setProvince(province.trim());
        order.setWard(ward.trim());
        order.setDetail(detail.trim());
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingFee(shippingFee != null ? shippingFee : BigDecimal.ZERO);

        orderItems.forEach(item -> item.setOrder(order));

        BigDecimal priceBeforeDiscount = orderItems.stream()
                .map(OrderItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setPriceBeforeDiscount(priceBeforeDiscount);

        BigDecimal finalPrice = priceBeforeDiscount.add(order.getShippingFee());
        order.setFinalPrice(finalPrice);

        order.setOrderItems(orderItems);

        Payment payment = new Payment();
        payment.setKeycloakId(currentKeycloakId);
        payment.setAmount(finalPrice);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setOrder(order);
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);
        savedOrder.setOrderCode(buildOrderCode(savedOrder.getId()));
        savedOrder = orderRepository.save(savedOrder);

        String checkoutUrl = null;

        if (paymentMethod == tea4life.order_service.model.constant.PaymentMethod.BANKING) {
            try {
                String fullIdStr = String.valueOf(savedOrder.getId());
                long payosOrderCode = Long.parseLong(fullIdStr.substring(Math.max(0, fullIdStr.length() - 8)));

                long amount = savedOrder.getFinalPrice().longValue();
                if (amount < 2000L) {
                    throw new IllegalArgumentException("Số tiền thanh toán chuyển khoản phải từ 2.000 VNĐ trở lên");
                }

                String description = "T4L " + payosOrderCode;

                CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                        .orderCode(payosOrderCode)
                        .amount(amount)
                        .description(description)
                        .returnUrl(frontendDomain + "/payment/success")
                        .cancelUrl(frontendDomain + "/payment/cancel")
                        .build();

                // Gọi hàm create của V2
                var data = payOS.paymentRequests().create(paymentRequest);
                checkoutUrl = data.getCheckoutUrl();

            } catch (Exception e) {
                log.error("Lỗi tạo link PAYOS cho Order ID: " + savedOrder.getId(), e);
                log.error(e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi PayOS: " + e.getMessage(), e);
            }
        }

        orderRecommendationEventPublisher.publishOrderPlacedAfterCommit(savedOrder);

        return toOrderResponse(savedOrder, checkoutUrl);
    }

    // =================================================
    // Mapping
    // =================================================

    // Hàm 1: Dành cho các API thông thường (không cần link thanh toán)
    private OrderResponse toOrderResponse(Order order) {
        return toOrderResponse(order, null);
    }

    // Hàm 2: Dành cho API Tạo đơn hàng (có link thanh toán)
    private OrderResponse toOrderResponse(Order order, String checkoutUrl) {
        List<OrderItemResponse> itemResponses = order.getOrderItems() == null
                ? List.of()
                : order.getOrderItems().stream()
                .map(this::toOrderItemResponse)
                .toList();

        return new OrderResponse(
                order.getId() == null ? null : order.getId().toString(),
                order.getOrderCode(),
                order.getReceiverName(),
                order.getPhone(),
                order.getProvince(),
                order.getWard(),
                order.getDetail(),
                order.getStore() == null || order.getStore().getId() == null ? null : order.getStore().getId().toString(),
                order.getStore() == null ? null : order.getStore().getName(),
                order.getStatus(),
                order.getPriceBeforeDiscount(),
                order.getFinalPrice(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getNote(),
                order.getCreatedAt(),
                itemResponses,
                checkoutUrl
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getProductId() == null ? null : orderItem.getProductId().toString(),
                orderItem.getProductName(),
                orderItem.getProductImageUrl(),
                fromSelectedOptionsSnapshot(orderItem.getSelectedOptionsSnapshot()),
                orderItem.getUnitPrice(),
                orderItem.getQuantity()
        );
    }

    // =================================================
    // Snapshot
    // =================================================

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

    private String toSelectedOptionsSnapshot(List<OrderItemOptionRequest> selectedOptions) {
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(selectedOptions);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "selectedOptions không hợp lệ", ex);
        }
    }

    // =================================================
    // Lookup / Validation
    // =================================================

    private String resolveCurrentKeycloakId() {
        UserContext context = UserContext.get();
        if (context == null || context.getKeycloakId() == null || context.getKeycloakId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được người dùng hiện tại");
        }
        return context.getKeycloakId().trim();
    }

    private Store resolveDefaultStore() {
        return storeRepository.findAll().stream()
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Chưa có cửa hàng hoạt động để nhận đơn"));
    }

    private Store resolveAssignedStore(Double latitude, Double longitude) {
        List<Store> activeStores = storeRepository.findAll().stream()
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .toList();

        if (activeStores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chưa có cửa hàng hoạt động để nhận đơn");
        }

        if (latitude == null || longitude == null) {
            return activeStores.get(0);
        }

        return activeStores.stream()
                .min((left, right) -> Double.compare(
                        calculateDistanceInKm(latitude, longitude, left.getLatitude(), left.getLongitude()),
                        calculateDistanceInKm(latitude, longitude, right.getLatitude(), right.getLongitude())
                ))
                .orElse(activeStores.get(0));
    }

    // =================================================
    // Utils
    // =================================================

    private Long parseProductId(String productId) {
        try {
            return Long.parseLong(productId.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId không hợp lệ", ex);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildOrderCode(Long orderId) {
        return "ORD-" + orderId;
    }

    private double calculateDistanceInKm(
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude
    ) {
        double earthRadiusKm = 6371.0;
        double latitudeDistance = Math.toRadians(destinationLatitude - originLatitude);
        double longitudeDistance = Math.toRadians(destinationLongitude - originLongitude);

        double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(Math.toRadians(originLatitude)) * Math.cos(Math.toRadians(destinationLatitude))
                * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}



