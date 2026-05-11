package tea4life.order_service.controller.payment;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tea4life.order_service.model.constant.OrderStatus;
import tea4life.order_service.model.constant.PaymentStatus;
import tea4life.order_service.model.order.Order;
import tea4life.order_service.model.payment.PaymentLog;
import tea4life.order_service.repository.OrderRepository;
import vn.payos.PayOS;
import vn.payos.model.webhooks.Webhook;

import java.math.BigDecimal;
import java.util.LinkedHashSet;

@RestController
@RequestMapping("/public/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PayOSWebhookController {
    PayOS payOS;
    OrderRepository orderRepository;

    @PostMapping("/webhook/payos")
    @Transactional
    public ResponseEntity<String> handlePayOSWebhook(@RequestBody Webhook webhook) {
        try {
            var data = payOS.webhooks().verify(webhook);

            log.info("Đã verify chữ ký thành công!");
            log.info("Dữ liệu nhận được - Code: {}, OrderCode: {}, Số tiền: {}", data.getCode(), data.getOrderCode(), data.getAmount());

            if ("00".equals(data.getCode())) {
                String shortOrderCode = String.valueOf(data.getOrderCode());
                log.info("Đang tìm đơn hàng có đuôi là: {}", shortOrderCode);

                Order order = orderRepository.findByOrderCodeEndingWith(shortOrderCode).orElse(null);

                if (order == null) {
                    log.warn("Không tìm thấy đơn hàng trong database!");
                } else {
                    log.info("Đã tìm thấy đơn hàng: {}, Trạng thái hiện tại: {}", order.getOrderCode(), order.getStatus());

                    if (order.getStatus() == OrderStatus.PENDING) {
                        order.setStatus(OrderStatus.PREPARING);
                        order.setPaymentStatus(PaymentStatus.COMPLETED);

                        if (order.getPayment() != null) {
                            order.getPayment().setStatus(PaymentStatus.COMPLETED);

                            PaymentLog paymentLog = new PaymentLog();
                            paymentLog.setGatewayTransactionId(data.getReference());
                            paymentLog.setAmount(BigDecimal.valueOf(data.getAmount()));
                            paymentLog.setDescription(data.getDescription());
                            paymentLog.setPayment(order.getPayment());

                            if (order.getPayment().getPaymentLogs() == null) {
                                order.getPayment().setPaymentLogs(new LinkedHashSet<>());
                            }
                            order.getPayment().getPaymentLogs().add(paymentLog);
                        }

                        orderRepository.save(order);
                        log.info("Hoàn tất - Đã gạch nợ thành công cho đơn hàng: {}", order.getOrderCode());
                    } else {
                        log.warn("Bỏ qua - Đơn hàng {} không ở trạng thái PENDING", order.getOrderCode());
                    }
                }
            } else {
                log.warn("Giao dịch chưa thành công, mã Code từ PayOS: {}", data.getCode());
            }

            return ResponseEntity.ok("{\"success\": true}");

        } catch (Exception e) {
            log.error("Lỗi xử lý Webhook PayOS: {}", e.getMessage());
            return ResponseEntity.badRequest().body("{\"success\": false}");
        }
    }
}