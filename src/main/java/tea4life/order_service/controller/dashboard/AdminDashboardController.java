package tea4life.order_service.controller.dashboard;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.response.dashboard.AdminDashboardResponse;
import tea4life.order_service.service.AdminDashboardService;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    AdminDashboardService adminDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(new ApiResponse<>(adminDashboardService.getDashboard()));
    }
}
