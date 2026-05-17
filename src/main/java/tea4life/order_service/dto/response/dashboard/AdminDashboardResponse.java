package tea4life.order_service.dto.response.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminDashboardResponse(
        GeneralStatsResponse generalStats,
        RevenueChartResponse revenueChart,
        List<TopProductResponse> topProducts,
        List<RecentOrderResponse> recentOrders,
        QuickStatsResponse quickStats
) {
    public record GeneralStatsResponse(
            StatItemResponse totalRevenue,
            StatItemResponse totalOrders,
            StatItemResponse newCustomers,
            StatItemResponse completionRate
    ) {
    }

    public record StatItemResponse(
            BigDecimal value,
            String change,
            String trend,
            String description
    ) {
    }

    public record RevenueChartResponse(
            RevenueSummaryResponse summary,
            List<RevenueChartPointResponse> chartData
    ) {
    }

    public record RevenueSummaryResponse(
            BigDecimal totalRevenue7Days,
            BigDecimal averagePerDay,
            String growthFromLastWeek
    ) {
    }

    public record RevenueChartPointResponse(
            String day,
            BigDecimal value,
            long orders
    ) {
    }

    public record TopProductResponse(
            String name,
            long sold,
            BigDecimal revenue,
            String trend
    ) {
    }

    public record RecentOrderResponse(
            String id,
            String customer,
            String product,
            BigDecimal amount,
            String status,
            Instant date
    ) {
    }

    public record QuickStatsResponse(
            long pending,
            long shipping,
            long completed,
            long cancelled
    ) {
    }
}
