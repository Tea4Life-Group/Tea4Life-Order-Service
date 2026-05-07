package tea4life.order_service.controller.driver;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.response.driver.DriverLocationResponse;
import tea4life.order_service.service.DriverLocationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/driver/orders")
public class DriverLocationController {

    DriverLocationService driverLocationService;

    @PostMapping("/{orderId}/location")
    public ResponseEntity<ApiResponse<DriverLocationResponse>> postLocation(@PathVariable Long orderId,
                                                                             @RequestBody DriverLocationResponse payload) {
        return ResponseEntity.ok(new ApiResponse<>(driverLocationService.saveLocation(orderId, payload)));
    }

    @GetMapping("/{orderId}/location/latest")
    public ResponseEntity<ApiResponse<DriverLocationResponse>> getLatest(@PathVariable Long orderId) {
        return ResponseEntity.ok(new ApiResponse<>(driverLocationService.getLatest(orderId)));
    }

    @GetMapping("/{orderId}/location/history")
    public ResponseEntity<ApiResponse<List<DriverLocationResponse>>> getHistory(@PathVariable Long orderId) {
        return ResponseEntity.ok(new ApiResponse<>(driverLocationService.getHistory(orderId)));
    }

}
