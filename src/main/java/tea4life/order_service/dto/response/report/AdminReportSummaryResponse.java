package tea4life.order_service.dto.response.report;

import java.math.BigDecimal;

public record AdminReportSummaryResponse(
        ReportMetricResponse totalProfit,
        ReportMetricResponse averageOrderValue,
        ReportMetricResponse totalOrders
) {
    public record ReportMetricResponse(
            BigDecimal value,
            String change,
            String trend
    ) {
    }
}
