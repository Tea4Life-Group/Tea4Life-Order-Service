package tea4life.order_service.voucher.service;

import org.springframework.stereotype.Service;
import tea4life.order_service.dto.request.CreateVoucherRequest;
import tea4life.order_service.dto.response.VoucherResponse;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 17/03/2026 - 10:45 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.order_service.voucher.service;
 */

@Service
public interface VoucherService {
    List<VoucherResponse> findAllVouchers();

    VoucherResponse findVoucherById(Long id);

    VoucherResponse saveVoucher(
            CreateVoucherRequest request
    );

    void deleteVoucherById(Long id);
}


