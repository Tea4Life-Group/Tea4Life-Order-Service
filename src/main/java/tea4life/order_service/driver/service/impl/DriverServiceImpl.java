package tea4life.order_service.driver.service.impl;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tea4life.order_service.client.UserInternalClient;
import tea4life.order_service.dto.request.driver.UpsertDriverRequest;
import tea4life.order_service.dto.response.driver.DriverResponse;
import tea4life.order_service.model.driver.Driver;
import tea4life.order_service.driver.repository.DriverRepository;
import tea4life.order_service.driver.service.DriverService;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Slf4j
public class DriverServiceImpl implements DriverService {

    // Repository
    DriverRepository driverRepository;
    UserInternalClient userInternalClient;

    static final String DRIVER_ROLE = "DRIVER";

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> findAllDrivers() {
        return driverRepository.findAll().stream()
                .map(this::toDriverResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse findDriverById(Long id) {
        return toDriverResponse(findDriverEntityById(id));
    }

    @Override
    public DriverResponse createDriver(UpsertDriverRequest request) {
        try {
            Driver driver = prepareDriverForCreate(request);
            DriverResponse response = toDriverResponse(driverRepository.save(driver));
            syncDriverRole(driver.getKeycloakId());
            return response;
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "keycloakId của driver đã tồn tại", ex);
        }
    }

    @Override
    public DriverResponse syncDriverFromUserRole(UpsertDriverRequest request) {
        try {
            Driver driver = prepareDriverForRoleSync(request);
            return toDriverResponse(driverRepository.save(driver));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể đồng bộ tài xế từ role DRIVER", ex);
        }
    }

    @Override
    public DriverResponse updateDriver(Long id, UpsertDriverRequest request) {
        Driver driver = findDriverEntityById(id);
        applyRequestToDriver(driver, request);

        try {
            DriverResponse response = toDriverResponse(driverRepository.save(driver));
            syncDriverRole(driver.getKeycloakId());
            return response;
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "keycloakId của driver đã tồn tại", ex);
        }
    }

    @Override
    public void deleteDriver(Long id) {
        Driver driver = findDriverEntityById(id);
        driverRepository.delete(driver);
        syncDeletedDriverRole(driver.getKeycloakId());
    }

    // =================================================
    // Lookup
    // =================================================

    private Driver findDriverEntityById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Driver với ID: " + id));
    }

    // =================================================
    // Mapping
    // =================================================

    private void applyRequestToDriver(Driver driver, UpsertDriverRequest request) {
        driver.setKeycloakId(normalizeKeycloakId(request.keycloakId()));
        driver.setFullName(request.fullName().trim());
        driver.setPhone(request.phone().trim());
    }

    private Driver prepareDriverForCreate(UpsertDriverRequest request) {
        Driver driver = findDriverIncludingDeleted(request, true);
        applyRequestToDriver(driver, request);
        return driver;
    }

    private Driver prepareDriverForRoleSync(UpsertDriverRequest request) {
        Driver driver = findDriverIncludingDeleted(request, false);
        applyRequestToDriver(driver, request);
        return driver;
    }

    private Driver findDriverIncludingDeleted(UpsertDriverRequest request, boolean rejectActiveDriver) {
        String keycloakId = normalizeKeycloakId(request.keycloakId());
        return driverRepository.findByKeycloakIdIncludingDeleted(keycloakId)
                .map(existing -> {
                    if (!existing.isDeleted() && rejectActiveDriver) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "keycloakId của driver đã tồn tại");
                    }
                    existing.setDeleted(false);
                    return existing;
                })
                .orElseGet(Driver::new);
    }

    private String normalizeKeycloakId(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keycloakId không được để trống");
        }
        return keycloakId.trim();
    }

    private void syncDriverRole(String keycloakId) {
        System.out.println("ĐÂY LÀ CODE MỚI NÈ, ĐÃ SỬA LỖI KHÔNG ĐỒNG BỘ ROLE DRIVER KHI TẠO/UPDATE DRIVER");
        try {
            var response = userInternalClient.assignRole(keycloakId, DRIVER_ROLE);
            if (response != null && response.getErrorCode() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        response.getErrorMessage() == null
                                ? "User Service trả lỗi khi đồng bộ role DRIVER"
                                : response.getErrorMessage()
                );
            }
        } catch (FeignException ex) {
            String responseBody = ex.contentUTF8();
            log.error(
                    "Không đồng bộ được role DRIVER cho keycloakId={}. User Service status={}, body={}",
                    keycloakId,
                    ex.status(),
                    responseBody,
                    ex
            );

            if (ex.status() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Không tìm thấy người dùng hoặc role DRIVER bên User Service",
                        ex
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Đã lưu tài xế thất bại vì không gọi được User Service để đồng bộ role DRIVER",
                    ex
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Không đồng bộ được role DRIVER cho keycloakId={}", keycloakId, ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Đã lưu tài xế thất bại vì không đồng bộ được role DRIVER",
                    ex
            );
        }
    }

    private void syncDeletedDriverRole(String keycloakId) {
        System.out.println("ĐÂY LÀ CODE MỚI NÈ, ĐÃ SỬA LỖI KHÔNG ĐỒNG BỘ ROLE DRIVER KHI TẠO/UPDATE DRIVER");
        try {
            var response = userInternalClient.downgradeDriverRoleToMember(keycloakId);
            if (response != null && response.getErrorCode() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        response.getErrorMessage() == null
                                ? "User Service trả lỗi khi đồng bộ role MEMBER"
                                : response.getErrorMessage()
                );
            }
        } catch (FeignException ex) {
            log.error(
                    "Không đồng bộ được role MEMBER sau khi xóa driver keycloakId={}. User Service status={}, body={}",
                    keycloakId,
                    ex.status(),
                    ex.contentUTF8(),
                    ex
            );
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Đã xóa tài xế thất bại vì không đồng bộ được role MEMBER",
                    ex
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Không đồng bộ được role MEMBER sau khi xóa driver keycloakId={}", keycloakId, ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Đã xóa tài xế thất bại vì không đồng bộ được role MEMBER",
                    ex
            );
        }
    }

    private DriverResponse toDriverResponse(Driver driver) {
        return new DriverResponse(
                driver.getId() == null ? null : driver.getId().toString(),
                driver.getKeycloakId(),
                driver.getFullName(),
                driver.getPhone()
        );
    }
}



