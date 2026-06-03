package tea4life.order_service.driver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tea4life.order_service.model.driver.Driver;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByKeycloakId(String keycloakId);

    @Query(
            value = "SELECT * FROM drivers WHERE keycloak_id = :keycloakId LIMIT 1",
            nativeQuery = true
    )
    Optional<Driver> findByKeycloakIdIncludingDeleted(@Param("keycloakId") String keycloakId);
}


