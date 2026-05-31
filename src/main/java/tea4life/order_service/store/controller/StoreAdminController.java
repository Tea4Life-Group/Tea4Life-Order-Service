package tea4life.order_service.store.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.request.store.UpsertStoreRequest;
import tea4life.order_service.dto.response.store.StoreResponse;
import tea4life.order_service.store.service.StoreService;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/stores")
public class StoreAdminController {

    StoreService storeService;

    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @RequestBody @Valid UpsertStoreRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(storeService.createStore(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @PathVariable Long id,
            @RequestBody @Valid UpsertStoreRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(storeService.updateStore(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<@NonNull Void>> deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
        return ResponseEntity.ok(new ApiResponse<>((Void) null));
    }
}



