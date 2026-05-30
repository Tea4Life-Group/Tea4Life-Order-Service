package tea4life.order_service.cart.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tea4life.order_service.dto.base.ApiResponse;
import tea4life.order_service.dto.request.cart.AddCartItemRequest;
import tea4life.order_service.dto.request.cart.UpdateCartItemRequest;
import tea4life.order_service.dto.response.cart.CartResponse;
import tea4life.order_service.dto.response.cart.RecentCartItemsResponse;
import tea4life.order_service.cart.service.CartService;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/cart")
public class CartController {

    CartService cartService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart() {
        return ResponseEntity.ok(new ApiResponse<>(cartService.getMyCart()));
    }

    @GetMapping("/me/items/recent")
    public ResponseEntity<ApiResponse<RecentCartItemsResponse>> getMyRecentCartItems() {
        return ResponseEntity.ok(new ApiResponse<>(cartService.getMyRecentCartItems()));
    }

    @PostMapping("/me/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToMyCart(
            @RequestBody @Valid AddCartItemRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.addItemToMyCart(request)));
    }

    @PutMapping("/me/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateMyCartItem(
            @PathVariable Long cartItemId,
            @RequestBody @Valid UpdateCartItemRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.updateMyCartItem(cartItemId, request)));
    }

    @DeleteMapping("/me/items/{cartItemId}")
    public ResponseEntity<ApiResponse<@NonNull Void>> removeMyCartItem(@PathVariable Long cartItemId) {
        cartService.removeMyCartItem(cartItemId);
        return ResponseEntity.ok(new ApiResponse<>((Void) null));
    }

    @DeleteMapping("/me/items")
    public ResponseEntity<ApiResponse<@NonNull Void>> clearMyCart() {
        cartService.clearMyCart();
        return ResponseEntity.ok(new ApiResponse<>((Void) null));
    }
}



