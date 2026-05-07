package tea4life.order_service.model.driver;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import tea4life.order_service.config.database.SnowflakeGenerated;
import tea4life.order_service.model.base.BaseEntity;
import tea4life.order_service.model.order.Order;

@Entity
@Table(name = "driver_locations")
@SQLDelete(sql = "UPDATE driver_locations SET is_deleted = 1 WHERE id = ?")
@SQLRestriction("is_deleted = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverLocation extends BaseEntity {

    @Id
    @SnowflakeGenerated
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    Order order;

    @Column(name = "driver_keycloak_id")
    String driverKeycloakId;

    @Column(nullable = false)
    Double latitude;

    @Column(nullable = false)
    Double longitude;

    @Column
    Double accuracy;

    @Column(name = "is_deleted", nullable = false)
    boolean isDeleted = false;
}
