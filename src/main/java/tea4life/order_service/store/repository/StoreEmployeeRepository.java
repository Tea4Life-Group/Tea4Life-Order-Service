package tea4life.order_service.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.order_service.model.store.StoreEmployee;

import java.util.List;
import java.util.Optional;

public interface StoreEmployeeRepository extends JpaRepository<StoreEmployee, Long> {

    List<StoreEmployee> findByStoreIdOrderByCreatedAtAsc(Long storeId);

    List<StoreEmployee> findByKeycloakIdOrderByCreatedAtAsc(String keycloakId);

    Optional<StoreEmployee> findByStoreIdAndKeycloakId(Long storeId, String keycloakId);
}


