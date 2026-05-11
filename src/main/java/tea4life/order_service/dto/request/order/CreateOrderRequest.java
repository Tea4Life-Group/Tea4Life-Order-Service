package tea4life.order_service.dto.request.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import tea4life.order_service.model.constant.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "receiverName không được để trống")
        String receiverName,

        @NotBlank(message = "phone không được để trống")
        String phone,

        @NotBlank(message = "province không được để trống")
        String province,

        @NotBlank(message = "ward không được để trống")
        String ward,

        @NotBlank(message = "detail không được để trống")
        String detail,

        Double latitude,

        Double longitude,

        @NotNull(message = "paymentMethod không được để trống")
        PaymentMethod paymentMethod,

        String voucherCode,

        @Valid
        @NotEmpty(message = "items không được để trống")
        List<OrderItemRequest> items,

        BigDecimal shippingFee
) {
}
