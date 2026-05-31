package tea4life.order_service.voucher.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.response.VoucherResponse;
import tea4life.order_service.voucher.service.VoucherService;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 17/03/2026 - 10:48 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.order_service.voucher.controller;
 */

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/public/vouchers")
public class VoucherController {

    final VoucherService voucherService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> findAllVouchers() {
        return ResponseEntity.ok(new ApiResponse<>(voucherService.findAllVouchers()));
    }
}



