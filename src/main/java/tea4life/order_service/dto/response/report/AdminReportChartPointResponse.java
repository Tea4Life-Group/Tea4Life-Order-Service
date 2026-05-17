package tea4life.order_service.dto.response.report;

import java.math.BigDecimal;

public record AdminReportChartPointResponse(
        String label,
        BigDecimal revenue,
        BigDecimal profit,
        long orders
) {
}
