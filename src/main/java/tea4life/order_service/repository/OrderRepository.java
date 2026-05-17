package tea4life.order_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import tea4life.order_service.model.constant.OrderStatus;
import tea4life.order_service.model.order.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    List<Order> findByStoreIdAndStatusOrderByCreatedAtDesc(Long storeId, OrderStatus status);

    Optional<Order> findByIdAndStoreId(Long id, Long storeId);

    List<Order> findByKeycloakIdOrderByCreatedAtDesc(String keycloakId);

    List<Order> findByKeycloakIdAndStatusOrderByCreatedAtDesc(String keycloakId, OrderStatus status);

    Optional<Order> findByIdAndKeycloakId(Long id, String keycloakId);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findByStatusAndDriverKeycloakIdIsNullOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findByStatusAndDriverKeycloakIdOrderByCreatedAtDesc(OrderStatus status, String driverKeycloakId);

    Optional<Order> findByIdAndDriverKeycloakId(Long id, String driverKeycloakId);
    Optional<Order> findByOrderCodeEndingWith(String suffix);

    List<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Order> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant start, Instant end);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant start, Instant end);

    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(OrderStatus status, Instant start, Instant end);

    long countByStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(List<OrderStatus> statuses, Instant start, Instant end);

    @Query("""
            select coalesce(sum(o.finalPrice), 0)
            from Order o
            where o.status = tea4life.order_service.model.constant.OrderStatus.COMPLETED
              and o.createdAt >= :start
              and o.createdAt < :end
            """)
    BigDecimal sumCompletedRevenueBetween(Instant start, Instant end);

    @Query("""
            select count(distinct o.keycloakId)
            from Order o
            where o.createdAt >= :start
              and o.createdAt < :end
              and o.createdAt = (
                  select min(firstOrder.createdAt)
                  from Order firstOrder
                  where firstOrder.keycloakId = o.keycloakId
              )
            """)
    long countFirstTimeCustomersBetween(Instant start, Instant end);

    @Query("""
            select coalesce(sum(item.subTotal), 0), coalesce(sum(item.quantity), 0)
            from OrderItem item
            join item.order o
            where item.productId = :productId
              and o.status = tea4life.order_service.model.constant.OrderStatus.COMPLETED
              and o.createdAt >= :start
              and o.createdAt < :end
            """)
    Object[] sumProductRevenueAndSoldBetween(Long productId, Instant start, Instant end);

    @Query("""
            select item.productId, item.productName, coalesce(sum(item.quantity), 0), coalesce(sum(item.subTotal), 0)
            from OrderItem item
            join item.order o
            where o.status = tea4life.order_service.model.constant.OrderStatus.COMPLETED
              and o.createdAt >= :start
              and o.createdAt < :end
            group by item.productId, item.productName
            order by coalesce(sum(item.subTotal), 0) desc, coalesce(sum(item.quantity), 0) desc
            """)
    List<Object[]> findTopProductsBetween(Instant start, Instant end, Pageable pageable);
}
