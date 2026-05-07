package tea4life.order_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.order_service.model.driver.DriverLocation;

import java.util.List;
import java.util.Optional;

public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {

    Optional<DriverLocation> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<DriverLocation> findByOrderIdOrderByCreatedAtAsc(Long orderId);

}
