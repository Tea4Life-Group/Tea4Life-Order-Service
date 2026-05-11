package tea4life.order_service.model.order;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import tea4life.order_service.config.database.SnowflakeGenerated;
import tea4life.order_service.model.voucher.VoucherOrder;
import tea4life.order_service.model.base.BaseEntity;
import tea4life.order_service.model.constant.OrderStatus;
import tea4life.order_service.model.constant.PaymentMethod;
import tea4life.order_service.model.constant.PaymentStatus;
import tea4life.order_service.model.payment.Payment;
import tea4life.order_service.model.store.Store;

import java.math.BigDecimal;
import java.util.Set;

/**
 * @author Le Tran Gia Huy
 * @created 10/03/2026 - 10:05 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.order_service.model.base
 */

@Entity
@Table(name = "orders")
@SQLDelete(sql = "UPDATE orders SET is_deleted = 1 WHERE id = ?")
@SQLRestriction("is_deleted = 0")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order extends BaseEntity {
    @Id
    @SnowflakeGenerated
    Long id;

    @Column(name = "order_code", unique = true)
    String orderCode;

    @Enumerated(EnumType.STRING)
    OrderStatus status;
    @Column(nullable = false, name = "user_id")
    String keycloakId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    Store store;
    @Column(nullable = false, name = "receiver_name")
    String receiverName;
    @Column(nullable = false)
    String phone;
    @Column(nullable = false)
    String province;
    @Column(nullable = false)
    String ward;
    @Column(nullable = false)
    String detail;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "payment_method")
    PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "payment_status")
    PaymentStatus paymentStatus;
    @Column(name = "driver_keycloak_id")
    String driverKeycloakId;
    String note;
    @Column(nullable = false, name = "price_before_discount")
    BigDecimal priceBeforeDiscount;
    @Column(nullable = false, name = "final_price")
    BigDecimal finalPrice;
    @Column(nullable = false, name = "is_deleted")
    boolean isDeleted = false;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<VoucherOrder> voucherOrders;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<OrderItem> orderItems;
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    Payment payment;

    @Column(nullable = false, name = "shipping_fee")
    BigDecimal shippingFee = BigDecimal.ZERO;
}
