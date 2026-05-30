package tea4life.order_service.store.service;

import tea4life.order_service.dto.request.store.AssignStoreEmployeeRequest;
import tea4life.order_service.dto.request.store.UpsertStoreRequest;
import tea4life.order_service.dto.response.store.StoreEmployeeResponse;
import tea4life.order_service.dto.response.store.StoreResponse;

import java.util.List;

public interface StoreService {

    List<StoreResponse> findAllStores();

    StoreResponse findStoreById(Long id);

    StoreResponse createStore(UpsertStoreRequest request);

    StoreResponse updateStore(Long id, UpsertStoreRequest request);

    void deleteStore(Long id);

    List<StoreEmployeeResponse> findStoreEmployees(Long storeId);

    StoreEmployeeResponse assignEmployee(Long storeId, AssignStoreEmployeeRequest request);

    void removeEmployee(Long storeId, String keycloakId);

    List<StoreResponse> findMyStores();
}


