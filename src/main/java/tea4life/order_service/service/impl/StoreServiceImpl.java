package tea4life.order_service.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tea4life.order_service.context.UserContext;
import tea4life.order_service.dto.request.store.AssignStoreEmployeeRequest;
import tea4life.order_service.dto.request.store.UpsertStoreRequest;
import tea4life.order_service.dto.response.store.StoreEmployeeResponse;
import tea4life.order_service.dto.response.store.StoreResponse;
import tea4life.order_service.model.store.Store;
import tea4life.order_service.model.store.StoreEmployee;
import tea4life.order_service.repository.StoreEmployeeRepository;
import tea4life.order_service.repository.StoreRepository;
import tea4life.order_service.service.StoreService;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class StoreServiceImpl implements StoreService {

    // Repository
    StoreRepository storeRepository;
    StoreEmployeeRepository storeEmployeeRepository;

    // ====================================
    // STORE ADMIN CRUD
    // ====================================

    @Override
    @Transactional(readOnly = true)
    public List<StoreResponse> findAllStores() {
        return storeRepository.findAll()
                .stream()
                .map(this::toStoreResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse findStoreById(Long id) {
        return toStoreResponse(findStoreEntityById(id));
    }

    @Override
    public StoreResponse createStore(UpsertStoreRequest request) {
        Store store = new Store();
        applyRequestToStore(store, request);
        return toStoreResponse(storeRepository.save(store));
    }

    @Override
    public StoreResponse updateStore(Long id, UpsertStoreRequest request) {
        Store store = findStoreEntityById(id);
        applyRequestToStore(store, request);
        return toStoreResponse(storeRepository.save(store));
    }

    @Override
    public void deleteStore(Long id) {
        Store store = findStoreEntityById(id);
        storeRepository.delete(store);
    }

    // ====================================
    // STORE ADMIN EMPLOYEE MANAGEMENT
    // ====================================

    @Override
    @Transactional(readOnly = true)
    public List<StoreEmployeeResponse> findStoreEmployees(Long storeId) {
        findStoreEntityById(storeId);
        return storeEmployeeRepository.findByStoreIdOrderByCreatedAtAsc(storeId).stream()
                .map(this::toStoreEmployeeResponse)
                .toList();
    }

    @Override
    public StoreEmployeeResponse assignEmployee(Long storeId, AssignStoreEmployeeRequest request) {
        Store store = findStoreEntityById(storeId);
        String keycloakId = normalizeRequiredKeycloakId(request.keycloakId());

        if (storeEmployeeRepository.findByStoreIdAndKeycloakId(storeId, keycloakId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nhân viên đã được gán vào chi nhánh này");
        }

        try {
            StoreEmployee storeEmployee = new StoreEmployee();
            storeEmployee.setStore(store);
            storeEmployee.setKeycloakId(keycloakId);
            return toStoreEmployeeResponse(storeEmployeeRepository.save(storeEmployee));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể gán nhân viên vào chi nhánh", ex);
        }
    }

    @Override
    public void removeEmployee(Long storeId, String keycloakId) {
        String normalizedKeycloakId = normalizeRequiredKeycloakId(keycloakId);
        StoreEmployee storeEmployee = storeEmployeeRepository.findByStoreIdAndKeycloakId(storeId, normalizedKeycloakId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy nhân viên trong chi nhánh này"
                ));
        storeEmployeeRepository.delete(storeEmployee);
    }

    // ====================================
    // USER STORES
    // ====================================

    @Override
    @Transactional(readOnly = true)
    public List<StoreResponse> findMyStores() {
        if (isAdmin()) {
            return findAllStores();
        }

        String keycloakId = resolveCurrentKeycloakId();
        return storeEmployeeRepository.findByKeycloakIdOrderByCreatedAtAsc(keycloakId).stream()
                .map(StoreEmployee::getStore)
                .map(this::toStoreResponse)
                .toList();
    }

    // =================================================
    // Lookup
    // =================================================

    private Store findStoreEntityById(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Store với ID: " + id));
    }

    // =================================================
    // Mapping
    // =================================================

    private void applyRequestToStore(Store store, UpsertStoreRequest request) {
        store.setName(request.name().trim());
        store.setAddress(request.address().trim());
        store.setLatitude(request.latitude());
        store.setLongitude(request.longitude());
    }

    private StoreResponse toStoreResponse(Store store) {
        List<StoreEmployeeResponse> employees = storeEmployeeRepository.findByStoreIdOrderByCreatedAtAsc(store.getId()).stream()
                .map(this::toStoreEmployeeResponse)
                .toList();

        return new StoreResponse(
                store.getId() == null ? null : store.getId().toString(),
                store.getName(),
                store.getAddress(),
                store.getLatitude(),
                store.getLongitude(),
                employees
        );
    }

    private StoreEmployeeResponse toStoreEmployeeResponse(StoreEmployee storeEmployee) {
        return new StoreEmployeeResponse(
                storeEmployee.getId() == null ? null : storeEmployee.getId().toString(),
                storeEmployee.getKeycloakId()
        );
    }

    // =================================================
    // Validation
    // =================================================

    private String resolveCurrentKeycloakId() {
        UserContext context = UserContext.get();
        if (context == null || context.getKeycloakId() == null || context.getKeycloakId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được người dùng hiện tại");
        }
        return context.getKeycloakId().trim();
    }

    private boolean isAdmin() {
        UserContext context = UserContext.get();
        return context != null && "ADMIN".equalsIgnoreCase(context.getRole());
    }

    private String normalizeRequiredKeycloakId(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keycloakId không được để trống");
        }
        return keycloakId.trim();
    }
}
