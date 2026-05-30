package tea4life.order_service.voucher.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.request.CreateVoucherRequest;
import tea4life.order_service.dto.response.VoucherResponse;
import tea4life.order_service.voucher.service.VoucherService;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 17/03/2026 - 11:03 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.order_service.voucher.controller;
 */

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/vouchers")
public class VoucherAdminController {

    final VoucherService voucherService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> findAllVouchers() {
        return ResponseEntity.ok(new ApiResponse<>(voucherService.findAllVouchers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> findVouchersById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(voucherService.findVoucherById(id)));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<VoucherResponse>> insertVouchers(
            @RequestBody @Valid CreateVoucherRequest request
    ) {
        try {
            VoucherResponse savedVoucher = voucherService.saveVoucher(
                    request
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(savedVoucher));
        } catch (DataIntegrityViolationException e) {
            // Lỗi do database (VD: vi phạm unique, thiếu trường bắt buộc...)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>("Lỗi dữ liệu: Vi phạm ràng buộc hoặc dữ liệu đã tồn tại.", null));
        } catch (Exception e) {
            // Lỗi hệ thống khác (VD: mất kết nối DB, lỗi runtime...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Lỗi server: Không thể thêm mới Voucher lúc này.", null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVouchers(
            @RequestBody @Valid CreateVoucherRequest request
    ) {
        try {
            VoucherResponse updatedVoucher = voucherService.saveVoucher(
                    request
            );
            return ResponseEntity.ok(new ApiResponse<>(updatedVoucher));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>("Lỗi dữ liệu: Vi phạm ràng buộc hoặc dữ liệu đã tồn tại.", null));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(e.getReason(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Lỗi server: Không thể cập nhật Voucher lúc này.", null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<@NonNull Void>> deleteVouchersById(
            @PathVariable Long id
    ) {
        try {
            voucherService.deleteVoucherById(id);
            return ResponseEntity.ok(new ApiResponse<>((Void) null));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(
                            "Lỗi dữ liệu: Không thể xóa Voucher do có liên kết với đơn hàng hoặc dữ liệu khác.",
                            null));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(e.getReason(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Lỗi server: Không thể xóa Voucher lúc này.", null));
        }
    }
}



