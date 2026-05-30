package tea4life.order_service.cart.service;

import tea4life.order_service.dto.request.cart.AddCartItemRequest;
import tea4life.order_service.dto.request.cart.UpdateCartItemRequest;
import tea4life.order_service.dto.response.cart.CartResponse;
import tea4life.order_service.dto.response.cart.RecentCartItemsResponse;

public interface CartService {

    CartResponse getMyCart();

    RecentCartItemsResponse getMyRecentCartItems();

    CartResponse addItemToMyCart(AddCartItemRequest request);

    CartResponse updateMyCartItem(Long cartItemId, UpdateCartItemRequest request);

    void removeMyCartItem(Long cartItemId);

    void clearMyCart();
}


