package tea4life.order_service.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.order_service.model.store.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {
}


