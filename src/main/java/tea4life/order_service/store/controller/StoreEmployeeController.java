package tea4life.order_service.store.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.request.store.AssignStoreEmployeeRequest;
import tea4life.order_service.dto.response.store.StoreEmployeeResponse;
import tea4life.order_service.dto.response.store.StoreResponse;
import tea4life.order_service.store.service.StoreService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/stores")
public class StoreEmployeeController {

    StoreService storeService;

    @GetMapping("/my-stores")
    public ResponseEntity<ApiResponse<List<StoreResponse>>> getMyStores() {
        return ResponseEntity.ok(new ApiResponse<>(storeService.findMyStores()));
    }

    @GetMapping("/{id}/employees")
    public ResponseEntity<ApiResponse<List<StoreEmployeeResponse>>> findStoreEmployees(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(storeService.findStoreEmployees(id)));
    }

    @PostMapping("/{id}/employees")
    public ResponseEntity<ApiResponse<StoreEmployeeResponse>> assignEmployee(
            @PathVariable Long id,
            @RequestBody @Valid AssignStoreEmployeeRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(storeService.assignEmployee(id, request)));
    }

    @DeleteMapping("/{id}/employees")
    public ResponseEntity<ApiResponse<@NonNull Void>> removeEmployee(
            @PathVariable Long id,
            @RequestParam("keycloakId") String keycloakId
    ) {
        storeService.removeEmployee(id, keycloakId);
        return ResponseEntity.ok(new ApiResponse<>((Void) null));
    }
}



