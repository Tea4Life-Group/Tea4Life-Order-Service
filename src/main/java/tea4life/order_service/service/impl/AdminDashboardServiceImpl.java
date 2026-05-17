package tea4life.order_service.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tea4life.order_service.dto.response.dashboard.AdminDashboardResponse;
import tea4life.order_service.model.constant.OrderStatus;
import tea4life.order_service.model.order.Order;
import tea4life.order_service.model.order.OrderItem;
import tea4life.order_service.repository.OrderRepository;
import tea4life.order_service.service.AdminDashboardService;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<OrderStatus> PENDING_STATUSES = List.of(
            OrderStatus.PENDING,
            OrderStatus.PREPARING,
            OrderStatus.READY_FOR_DELIVERY
    );

    OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate currentMonthStartDate = today.withDayOfMonth(1);
        LocalDate previousMonthStartDate = currentMonthStartDate.minusMonths(1);
        LocalDate nextMonthStartDate = currentMonthStartDate.plusMonths(1);

        Instant currentMonthStart = startOfDay(currentMonthStartDate);
        Instant previousMonthStart = startOfDay(previousMonthStartDate);
        Instant nextMonthStart = startOfDay(nextMonthStartDate);
        Instant todayStart = startOfDay(today);
        Instant tomorrowStart = startOfDay(today.plusDays(1));

        AdminDashboardResponse.GeneralStatsResponse generalStats = buildGeneralStats(
                currentMonthStart,
                nextMonthStart,
                previousMonthStart,
                currentMonthStart
        );

        AdminDashboardResponse.RevenueChartResponse revenueChart = buildRevenueChart(today);
        List<AdminDashboardResponse.TopProductResponse> topProducts = buildTopProducts(currentMonthStart, nextMonthStart);
        List<AdminDashboardResponse.RecentOrderResponse> recentOrders = buildRecentOrders();
        AdminDashboardResponse.QuickStatsResponse quickStats = buildQuickStats(todayStart, tomorrowStart);

        return new AdminDashboardResponse(generalStats, revenueChart, topProducts, recentOrders, quickStats);
    }

    private AdminDashboardResponse.GeneralStatsResponse buildGeneralStats(
            Instant currentStart,
            Instant currentEnd,
            Instant previousStart,
            Instant previousEnd
    ) {
        BigDecimal currentRevenue = orderRepository.sumCompletedRevenueBetween(currentStart, currentEnd);
        BigDecimal previousRevenue = orderRepository.sumCompletedRevenueBetween(previousStart, previousEnd);

        long currentOrders = orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(currentStart, currentEnd);
        long previousOrders = orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(previousStart, previousEnd);

        long currentCustomers = orderRepository.countFirstTimeCustomersBetween(currentStart, currentEnd);
        long previousCustomers = orderRepository.countFirstTimeCustomersBetween(previousStart, previousEnd);

        long currentCompleted = orderRepository.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                OrderStatus.COMPLETED,
                currentStart,
                currentEnd
        );
        long previousCompleted = orderRepository.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                OrderStatus.COMPLETED,
                previousStart,
                previousEnd
        );

        BigDecimal currentCompletionRate = percentage(BigDecimal.valueOf(currentCompleted), BigDecimal.valueOf(currentOrders));
        BigDecimal previousCompletionRate = percentage(BigDecimal.valueOf(previousCompleted), BigDecimal.valueOf(previousOrders));

        return new AdminDashboardResponse.GeneralStatsResponse(
                stat(currentRevenue, previousRevenue, "So với tháng trước"),
                stat(BigDecimal.valueOf(currentOrders), BigDecimal.valueOf(previousOrders), "Trong tháng này"),
                stat(BigDecimal.valueOf(currentCustomers), BigDecimal.valueOf(previousCustomers), "Khách có đơn đầu tiên"),
                stat(currentCompletionRate, previousCompletionRate, "Đơn hàng thành công")
        );
    }

    private AdminDashboardResponse.RevenueChartResponse buildRevenueChart(LocalDate today) {
        LocalDate chartStartDate = today.minusDays(6);
        LocalDate chartEndDate = today.plusDays(1);
        LocalDate previousWeekStartDate = chartStartDate.minusDays(7);

        Instant chartStart = startOfDay(chartStartDate);
        Instant chartEnd = startOfDay(chartEndDate);
        Instant previousWeekStart = startOfDay(previousWeekStartDate);

        BigDecimal totalRevenue = orderRepository.sumCompletedRevenueBetween(chartStart, chartEnd);
        BigDecimal previousWeekRevenue = orderRepository.sumCompletedRevenueBetween(previousWeekStart, chartStart);

        List<AdminDashboardResponse.RevenueChartPointResponse> chartData = chartStartDate
                .datesUntil(chartEndDate)
                .map(date -> {
                    Instant start = startOfDay(date);
                    Instant end = startOfDay(date.plusDays(1));
                    BigDecimal revenue = orderRepository.sumCompletedRevenueBetween(start, end);
                    long orders = orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
                    return new AdminDashboardResponse.RevenueChartPointResponse(dayLabel(date), revenue, orders);
                })
                .toList();

        AdminDashboardResponse.RevenueSummaryResponse summary = new AdminDashboardResponse.RevenueSummaryResponse(
                totalRevenue,
                totalRevenue.divide(BigDecimal.valueOf(7), 0, RoundingMode.HALF_UP),
                changeText(totalRevenue, previousWeekRevenue)
        );

        return new AdminDashboardResponse.RevenueChartResponse(summary, chartData);
    }

    private List<AdminDashboardResponse.TopProductResponse> buildTopProducts(Instant currentStart, Instant currentEnd) {
        Instant previousStart = currentStart.atZone(BUSINESS_ZONE).minusMonths(1).toInstant();

        return orderRepository.findTopProductsBetween(currentStart, currentEnd, PageRequest.of(0, 5))
                .stream()
                .map(row -> {
                    Long productId = ((Number) row[0]).longValue();
                    String productName = (String) row[1];
                    long sold = ((Number) row[2]).longValue();
                    BigDecimal revenue = toBigDecimal(row[3]);
                    Object[] previous = orderRepository.sumProductRevenueAndSoldBetween(productId, previousStart, currentStart);
                    BigDecimal previousRevenue = previous == null || previous.length == 0
                            ? BigDecimal.ZERO
                            : toBigDecimal(previous[0]);

                    return new AdminDashboardResponse.TopProductResponse(
                            productName,
                            sold,
                            revenue,
                            changeText(revenue, previousRevenue)
                    );
                })
                .toList();
    }

    private List<AdminDashboardResponse.RecentOrderResponse> buildRecentOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5))
                .stream()
                .map(order -> new AdminDashboardResponse.RecentOrderResponse(
                        displayOrderId(order),
                        order.getReceiverName(),
                        mainProductName(order),
                        order.getFinalPrice(),
                        statusLabel(order.getStatus()),
                        order.getCreatedAt()
                ))
                .toList();
    }

    private AdminDashboardResponse.QuickStatsResponse buildQuickStats(Instant todayStart, Instant tomorrowStart) {
        long pending = orderRepository.countByStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                PENDING_STATUSES,
                todayStart,
                tomorrowStart
        );
        long shipping = orderRepository.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                OrderStatus.DELIVERING,
                todayStart,
                tomorrowStart
        );
        long completed = orderRepository.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                OrderStatus.COMPLETED,
                todayStart,
                tomorrowStart
        );
        long cancelled = orderRepository.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                OrderStatus.CANCELLED,
                todayStart,
                tomorrowStart
        );

        return new AdminDashboardResponse.QuickStatsResponse(pending, shipping, completed, cancelled);
    }

    private AdminDashboardResponse.StatItemResponse stat(BigDecimal current, BigDecimal previous, String description) {
        String change = changeText(current, previous);
        return new AdminDashboardResponse.StatItemResponse(
                current,
                change,
                change.startsWith("-") ? "down" : "up",
                description
        );
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 1, RoundingMode.HALF_UP);
    }

    private String changeText(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current.compareTo(BigDecimal.ZERO) == 0) {
                return "+0%";
            }
            return "+100%";
        }

        BigDecimal change = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 1, RoundingMode.HALF_UP);
        return (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + change.stripTrailingZeros().toPlainString() + "%";
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private String dayLabel(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }

    private String displayOrderId(Order order) {
        String code = order.getOrderCode();
        if (code == null || code.isBlank()) {
            return "#ORD-" + order.getId();
        }
        return code.startsWith("#") ? code : "#" + code;
    }

    private String mainProductName(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return "N/A";
        }
        return order.getOrderItems()
                .stream()
                .max(Comparator.comparing(OrderItem::getSubTotal))
                .map(OrderItem::getProductName)
                .orElse("N/A");
    }

    private String statusLabel(OrderStatus status) {
        return switch (status) {
            case COMPLETED -> "Đã giao";
            case DELIVERING -> "Đang giao";
            case CANCELLED -> "Đã hủy";
            case PENDING, PREPARING, READY_FOR_DELIVERY -> "Đang xử lý";
        };
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Object[] values) {
            return values.length == 0 ? BigDecimal.ZERO : toBigDecimal(values[0]);
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0 ? BigDecimal.ZERO : toBigDecimal(Array.get(value, 0));
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            if (number instanceof Double || number instanceof Float) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            return new BigDecimal(number.toString());
        }
        String text = value.toString().trim();
        return text.isBlank() ? BigDecimal.ZERO : new BigDecimal(text);
    }
}
