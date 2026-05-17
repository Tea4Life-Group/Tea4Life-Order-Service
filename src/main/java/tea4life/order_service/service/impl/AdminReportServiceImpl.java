package tea4life.order_service.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tea4life.order_service.dto.response.report.AdminReportChartPointResponse;
import tea4life.order_service.dto.response.report.AdminReportSummaryResponse;
import tea4life.order_service.model.constant.OrderStatus;
import tea4life.order_service.model.order.Order;
import tea4life.order_service.repository.OrderRepository;
import tea4life.order_service.service.AdminReportService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminReportServiceImpl implements AdminReportService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

    OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminReportSummaryResponse getSummary() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate currentMonthStartDate = today.withDayOfMonth(1);
        LocalDate nextMonthStartDate = currentMonthStartDate.plusMonths(1);
        LocalDate previousMonthStartDate = currentMonthStartDate.minusMonths(1);

        Instant currentStart = startOfDay(currentMonthStartDate);
        Instant currentEnd = startOfDay(nextMonthStartDate);
        Instant previousStart = startOfDay(previousMonthStartDate);
        Instant previousEnd = currentStart;

        List<Order> currentOrders = completedOrders(currentStart, currentEnd);
        List<Order> previousOrders = completedOrders(previousStart, previousEnd);

        BigDecimal currentRevenue = revenue(currentOrders);
        BigDecimal previousRevenue = revenue(previousOrders);
        BigDecimal currentProfit = profit(currentOrders);
        BigDecimal previousProfit = profit(previousOrders);
        BigDecimal currentAov = averageOrderValue(currentRevenue, currentOrders.size());
        BigDecimal previousAov = averageOrderValue(previousRevenue, previousOrders.size());

        return new AdminReportSummaryResponse(
                metric(currentProfit, previousProfit),
                metric(currentAov, previousAov),
                metric(BigDecimal.valueOf(currentOrders.size()), BigDecimal.valueOf(previousOrders.size()))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminReportChartPointResponse> getChart(String period) {
        ReportPeriod reportPeriod = ReportPeriod.from(period);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        return switch (reportPeriod) {
            case SEVEN_DAYS -> buildDailyChart(today.minusDays(6), today.plusDays(1), true);
            case THIRTY_DAYS -> buildDailyChart(today.minusDays(29), today.plusDays(1), false);
            case YEAR -> buildMonthlyChart(today.withDayOfYear(1), today.plusDays(1));
        };
    }

    @Override
    @SneakyThrows
    @Transactional(readOnly = true)
    public byte[] export(String period) {
        List<AdminReportChartPointResponse> rows = getChart(period);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bao cao doanh thu");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            String[] columns = {"Thoi gian", "Doanh thu", "Loi nhuan", "So don"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }

            for (int i = 0; i < rows.size(); i++) {
                AdminReportChartPointResponse item = rows.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(item.label());
                row.createCell(1).setCellValue(item.revenue().doubleValue());
                row.createCell(2).setCellValue(item.profit().doubleValue());
                row.createCell(3).setCellValue(item.orders());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private List<AdminReportChartPointResponse> buildDailyChart(LocalDate startDate, LocalDate endDate, boolean useWeekdayLabel) {
        return startDate.datesUntil(endDate)
                .map(date -> {
                    Instant start = startOfDay(date);
                    Instant end = startOfDay(date.plusDays(1));
                    List<Order> orders = completedOrders(start, end);
                    return new AdminReportChartPointResponse(
                            useWeekdayLabel ? weekdayLabel(date) : date.format(DAY_FORMATTER),
                            revenue(orders),
                            profit(orders),
                            orders.size()
                    );
                })
                .toList();
    }

    private List<AdminReportChartPointResponse> buildMonthlyChart(LocalDate yearStartDate, LocalDate endDate) {
        return yearStartDate.withDayOfMonth(1)
                .datesUntil(endDate.withDayOfMonth(1).plusMonths(1), Period.ofMonths(1))
                .map(month -> {
                    Instant start = startOfDay(month);
                    Instant end = startOfDay(month.plusMonths(1));
                    List<Order> orders = completedOrders(start, end);
                    return new AdminReportChartPointResponse(
                            "Tháng " + month.getMonthValue(),
                            revenue(orders),
                            profit(orders),
                            orders.size()
                    );
                })
                .toList();
    }

    private List<Order> completedOrders(Instant start, Instant end) {
        return orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end)
                .stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .toList();
    }

    private BigDecimal revenue(List<Order> orders) {
        return orders.stream()
                .map(Order::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal profit(List<Order> orders) {
        return orders.stream()
                .map(order -> nullSafe(order.getFinalPrice()).subtract(nullSafe(order.getShippingFee())))
                .map(value -> value.max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal averageOrderValue(BigDecimal revenue, int orderCount) {
        if (orderCount == 0) {
            return BigDecimal.ZERO;
        }
        return revenue.divide(BigDecimal.valueOf(orderCount), 0, RoundingMode.HALF_UP);
    }

    private AdminReportSummaryResponse.ReportMetricResponse metric(BigDecimal current, BigDecimal previous) {
        String change = changeText(current, previous);
        return new AdminReportSummaryResponse.ReportMetricResponse(
                current,
                change,
                change.startsWith("-") ? "down" : "up"
        );
    }

    private String changeText(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? "+0%" : "+100%";
        }
        BigDecimal change = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 1, RoundingMode.HALF_UP);
        return (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + change.stripTrailingZeros().toPlainString() + "%";
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private String weekdayLabel(LocalDate date) {
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

    private enum ReportPeriod {
        SEVEN_DAYS,
        THIRTY_DAYS,
        YEAR;

        static ReportPeriod from(String value) {
            if ("30days".equalsIgnoreCase(value)) {
                return THIRTY_DAYS;
            }
            if ("year".equalsIgnoreCase(value)) {
                return YEAR;
            }
            return SEVEN_DAYS;
        }
    }
}
