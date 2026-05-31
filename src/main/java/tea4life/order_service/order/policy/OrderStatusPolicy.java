package tea4life.order_service.order.policy;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tea4life.order_service.model.constant.OrderStatus;
import tea4life.order_service.model.order.Order;

@Component
public class OrderStatusPolicy {

    public void ensureCustomerCanCancel(Order order) {
        ensureStatus(order, OrderStatus.PENDING, "Chỉ có thể hủy đơn ở trạng thái PENDING");
    }

    public void ensureStoreCanCancel(Order order) {
        if (order.getStatus() == OrderStatus.DELIVERING
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể hủy đơn ở trạng thái hiện tại");
        }
    }

    public void ensureCanCompleteDelivery(Order order) {
        ensureStatus(order, OrderStatus.DELIVERING, "Chỉ có thể hoàn tất đơn ở trạng thái DELIVERING");
    }

    private void ensureStatus(Order order, OrderStatus expectedStatus, String message) {
        if (order.getStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }
}
