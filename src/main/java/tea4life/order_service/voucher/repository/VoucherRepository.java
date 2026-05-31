package tea4life.order_service.voucher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tea4life.order_service.model.voucher.Voucher;

import java.util.Optional;

/**
 * @author Le Tran Gia Huy
 * @created 17/03/2026 - 10:44 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.order_service.voucher.repository;
 */

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    @Override
    Optional<Voucher> findById(Long id);
}
