package tea4life.order_service.service;

import tea4life.order_service.dto.response.report.AdminReportChartPointResponse;
import tea4life.order_service.dto.response.report.AdminReportSummaryResponse;

import java.util.List;

public interface AdminReportService {

    AdminReportSummaryResponse getSummary();

    List<AdminReportChartPointResponse> getChart(String period);

    byte[] export(String period);
}
