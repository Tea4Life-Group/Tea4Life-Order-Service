package tea4life.order_service.voucher.service.impl;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tea4life.order_service.client.StorageClient;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.request.CreateVoucherRequest;
import tea4life.order_service.dto.request.FileMoveRequest;
import tea4life.order_service.dto.response.VoucherResponse;
import tea4life.order_service.model.voucher.Voucher;
import tea4life.order_service.voucher.repository.VoucherRepository;
import tea4life.order_service.voucher.service.VoucherService;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 17/03/2026 - 10:45 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.order_service.voucher.service.impl;
 */

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class VoucherServiceImpl implements VoucherService {
    VoucherRepository voucherRepository;
    StorageClient storageClient;
    KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic.storage-delete-file}")
    @NonFinal
    String storageDeleteFileTopic;

    public List<VoucherResponse> findAllVouchers() {
        return voucherRepository.findAll().stream().map(this::toVoucherResponse).toList();
    }

    public VoucherResponse findVoucherById(Long id) {
        return voucherRepository.findById(id).map(this::toVoucherResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Voucher với ID: " + id));
    }

    public Voucher findVoucherByIdWithoutMapping(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Voucher với ID: " + id));
    }

    public VoucherResponse saveVoucher(
            CreateVoucherRequest request
    ) {
        Voucher voucher;
        String oldImageUrl = null;
        Long id = null;
        if (hasText(request.id())) {
            try {
                id = Long.parseLong(request.id());
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng ID không hợp lệ: " + request.id());
            }
        }
        if (id != null) {
            Long finalId = id;
            voucher = voucherRepository.findById(finalId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Voucher với ID: " + finalId));
            oldImageUrl = voucher.getImgUrl();
        } else {
            voucher = new Voucher();
        }
        applyRequestBodyToVoucher(voucher, request);
        if (voucher.getId() == null) {
            voucher = voucherRepository.save(voucher);
        }
        if (hasText(request.imgKey()) && !request.imgKey().equals(oldImageUrl)) {
            String destinationPath = "vouchers/items/" + voucher.getId();
            ApiResponse<String> storageResponse = storageClient.confirmFile(new FileMoveRequest(request.imgKey(), destinationPath));
            if (storageResponse.getErrorCode() != null) {
                throw new RuntimeException("Loi di chuyen file: " + storageResponse.getErrorMessage());
            }
            voucher.setImgUrl(storageResponse.getData());
        }
        voucher = voucherRepository.save(voucher);
        if (hasText(request.imgKey()) && oldImageUrl != null && !oldImageUrl.equals(voucher.getImgUrl())) {
            publishStorageDelete(oldImageUrl);
        }
        return toVoucherResponse(voucher);
    }

    public void deleteVoucherById(Long id) {
        Voucher voucher = findVoucherByIdWithoutMapping(id);
//        String imgUrl = voucher.getImgUrl();
        voucherRepository.deleteById(id);
//        publishStorageDelete(imgUrl);
    }

    private VoucherResponse toVoucherResponse(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId().toString(),
                voucher.getDiscountPercentage(),
                voucher.getMinOrderAmount().toString(),
                voucher.getMaxDiscountAmount().toString(),
                voucher.getDescription(),
                voucher.getImgUrl()
        );
    }

    private void applyRequestBodyToVoucher(Voucher voucher, CreateVoucherRequest request) {
        voucher.setDiscountPercentage(request.discountPercentage());
        voucher.setMinOrderAmount(request.minOrderAmount());
        voucher.setMaxDiscountAmount(request.maxDiscountAmount());
        voucher.setDescription(request.description());
        voucher.setImgUrl(request.imgKey());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void publishStorageDelete(String fileUrl) {
        if (hasText(fileUrl)) {
            kafkaTemplate.send(storageDeleteFileTopic, fileUrl);
        }
    }
}



