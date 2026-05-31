package tea4life.order_service.driver.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.request.driver.UpsertDriverRequest;
import tea4life.order_service.dto.response.driver.DriverResponse;
import tea4life.order_service.driver.service.DriverService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/drivers")
public class DriverAdminController {

    DriverService driverService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DriverResponse>>> findAllDrivers() {
        return ResponseEntity.ok(new ApiResponse<>(driverService.findAllDrivers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> findDriverById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(driverService.findDriverById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(@RequestBody @Valid UpsertDriverRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(driverService.createDriver(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(
            @PathVariable Long id,
            @RequestBody @Valid UpsertDriverRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(driverService.updateDriver(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<@NonNull Void>> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.ok(new ApiResponse<>((Void) null));
    }
}



