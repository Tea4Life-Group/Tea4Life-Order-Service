package tea4life.order_service.store.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.response.store.StoreResponse;
import tea4life.order_service.store.service.StoreService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoreController {

    StoreService storeService;

    @GetMapping("/public/stores")
    public ResponseEntity<ApiResponse<List<StoreResponse>>> findAllStores() {
        return ResponseEntity.ok(new ApiResponse<>(storeService.findAllStores()));
    }

    @GetMapping("/stores/{id}")
    public ResponseEntity<ApiResponse<StoreResponse>> findStoreById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(storeService.findStoreById(id)));
    }
}



