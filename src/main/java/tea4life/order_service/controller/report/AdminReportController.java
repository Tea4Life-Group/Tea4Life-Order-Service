package tea4life.order_service.controller.report;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.response.report.AdminReportChartPointResponse;
import tea4life.order_service.dto.response.report.AdminReportSummaryResponse;
import tea4life.order_service.service.AdminReportService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/reports")
public class AdminReportController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    AdminReportService adminReportService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminReportSummaryResponse>> getSummary() {
        return ResponseEntity.ok(new ApiResponse<>(adminReportService.getSummary()));
    }

    @GetMapping("/chart")
    public ResponseEntity<ApiResponse<List<AdminReportChartPointResponse>>> getChart(
            @RequestParam(defaultValue = "7days") String period
    ) {
        return ResponseEntity.ok(new ApiResponse<>(adminReportService.getChart(period)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "7days") String period
    ) {
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("bao-cao-doanh-thu.xlsx")
                                .build()
                                .toString()
                )
                .body(adminReportService.export(period));
    }
}
