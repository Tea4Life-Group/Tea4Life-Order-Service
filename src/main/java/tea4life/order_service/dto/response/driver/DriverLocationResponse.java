package tea4life.order_service.dto.response.driver;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverLocationResponse {
    String id;
    String orderId;
    String driverKeycloakId;
    Double latitude;
    Double longitude;
    Double accuracy;
    Instant recordedAt;
}
