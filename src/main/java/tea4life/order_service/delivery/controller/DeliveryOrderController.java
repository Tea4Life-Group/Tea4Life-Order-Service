package tea4life.order_service.delivery.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.response.order.DeliveryOrderResponse;
import tea4life.order_service.delivery.service.DeliveryOrderService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/driver/orders")
public class DeliveryOrderController {

    DeliveryOrderService deliveryOrderService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<DeliveryOrderResponse>>> findAvailableOrders() {
        return ResponseEntity.ok(new ApiResponse<>(deliveryOrderService.findAvailableOrders()));
    }

    @GetMapping("/shipping")
    public ResponseEntity<ApiResponse<List<DeliveryOrderResponse>>> findShippingOrders() {
        return ResponseEntity.ok(new ApiResponse<>(deliveryOrderService.findShippingOrders()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<DeliveryOrderResponse>> findOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(new ApiResponse<>(deliveryOrderService.findOrderById(orderId)));
    }

    @PostMapping("/{orderId}/pickup")
    public ResponseEntity<ApiResponse<DeliveryOrderResponse>> pickupOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(new ApiResponse<>(deliveryOrderService.pickupOrder(orderId)));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<ApiResponse<DeliveryOrderResponse>> completeOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(new ApiResponse<>(deliveryOrderService.completeOrder(orderId)));
    }
}



