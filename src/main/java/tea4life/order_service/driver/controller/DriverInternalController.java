package tea4life.order_service.driver.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.request.driver.UpsertDriverRequest;
import tea4life.order_service.dto.response.driver.DriverResponse;
import tea4life.order_service.driver.service.DriverService;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/internal/drivers")
public class DriverInternalController {

    DriverService driverService;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<DriverResponse>> syncDriverFromUserRole(
            @RequestBody @Valid UpsertDriverRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(driverService.syncDriverFromUserRole(request)));
    }
}
