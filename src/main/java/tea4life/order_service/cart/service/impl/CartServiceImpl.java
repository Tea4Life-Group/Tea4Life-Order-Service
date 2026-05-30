package tea4life.order_service.cart.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tea4life.order_service.context.UserContext;
import tea4life.order_service.dto.request.cart.AddCartItemRequest;
import tea4life.order_service.dto.request.cart.CartItemOptionSelectionRequest;
import tea4life.order_service.dto.request.cart.UpdateCartItemRequest;
import tea4life.order_service.dto.response.cart.CartItemOptionSelectionResponse;
import tea4life.order_service.dto.response.cart.CartItemResponse;
import tea4life.order_service.dto.response.cart.CartResponse;
import tea4life.order_service.dto.response.cart.RecentCartItemsResponse;
import tea4life.order_service.model.cart.Cart;
import tea4life.order_service.model.cart.CartItem;
import tea4life.order_service.cart.repository.CartItemRepository;
import tea4life.order_service.cart.repository.CartRepository;
import tea4life.order_service.cart.service.CartService;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class CartServiceImpl implements CartService {

    CartRepository cartRepository;
    CartItemRepository cartItemRepository;
    ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getMyCart() {
        Cart cart = getOrCreateCart(resolveCurrentKeycloakId());
        return toCartResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public RecentCartItemsResponse getMyRecentCartItems() {
        Cart cart = getOrCreateCart(resolveCurrentKeycloakId());
        List<CartItem> cartItems = getSortedCartItems(cart);

        List<CartItemResponse> recentItems = cartItems.stream()
                .limit(3)
                .map(this::toCartItemResponse)
                .toList();

        int totalItems = cartItems.stream()
                .map(CartItem::getQuantity)
                .reduce(0, Integer::sum);

        return new RecentCartItemsResponse(recentItems, totalItems);
    }

    @Override
    public CartResponse addItemToMyCart(AddCartItemRequest request) {
        Cart cart = getOrCreateCart(resolveCurrentKeycloakId());

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        applyAddRequestToCartItem(cartItem, request);
        cartItemRepository.save(cartItem);

        return toCartResponse(cartRepository.findById(cart.getId()).orElse(cart));
    }

    @Override
    public CartResponse updateMyCartItem(Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(resolveCurrentKeycloakId());
        CartItem cartItem = findCartItem(cart.getId(), cartItemId);

        cartItem.setQuantity(request.quantity());
        cartItem.setSubTotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(request.quantity())));
        cartItemRepository.save(cartItem);

        return toCartResponse(cartRepository.findById(cart.getId()).orElse(cart));
    }

    @Override
    public void removeMyCartItem(Long cartItemId) {
        Cart cart = getOrCreateCart(resolveCurrentKeycloakId());
        CartItem cartItem = findCartItem(cart.getId(), cartItemId);
        cartItemRepository.delete(cartItem);
    }

    @Override
    public void clearMyCart() {
        Cart cart = getOrCreateCart(resolveCurrentKeycloakId());
        if (cart.getCartItems() != null) {
            cart.getCartItems().clear();
        }
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(String keycloakId) {
        return cartRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> cartRepository.save(buildEmptyCart(keycloakId)));
    }

    private Cart buildEmptyCart(String keycloakId) {
        Cart cart = new Cart();
        cart.setKeycloakId(keycloakId);
        cart.setCartItems(new LinkedHashSet<>());
        return cart;
    }

    private CartItem findCartItem(Long cartId, Long cartItemId) {
        return cartItemRepository.findByIdAndCartId(cartItemId, cartId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Cart Item với ID: " + cartItemId));
    }

    private void applyAddRequestToCartItem(CartItem cartItem, AddCartItemRequest request) {
        cartItem.setProductId(parseProductId(request.productId()));
        cartItem.setProductName(request.productName().trim());
        cartItem.setProductImageUrl(trimToNull(request.productImageUrl()));
        cartItem.setSelectedOptionsSnapshot(toSelectedOptionsSnapshot(request.selectedOptions()));
        cartItem.setUnitPrice(request.unitPrice());
        cartItem.setQuantity(request.quantity());
        cartItem.setSubTotal(request.unitPrice().multiply(BigDecimal.valueOf(request.quantity())));
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = getSortedCartItems(cart).stream()
                .map(this::toCartItemResponse)
                .toList();

        int totalItems = itemResponses.stream()
                .map(CartItemResponse::quantity)
                .reduce(0, Integer::sum);

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::subTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                cart.getId() == null ? null : cart.getId().toString(),
                cart.getKeycloakId(),
                itemResponses,
                totalItems,
                totalAmount
        );
    }

    private List<CartItem> getSortedCartItems(Cart cart) {
        return cart.getCartItems() == null
                ? List.of()
                : cart.getCartItems().stream()
                .sorted(Comparator.comparing(CartItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CartItem::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();
    }

    private CartItemResponse toCartItemResponse(CartItem cartItem) {
        return new CartItemResponse(
                cartItem.getId() == null ? null : cartItem.getId().toString(),
                cartItem.getProductId() == null ? null : cartItem.getProductId().toString(),
                cartItem.getProductName(),
                cartItem.getProductImageUrl(),
                fromSelectedOptionsSnapshot(cartItem.getSelectedOptionsSnapshot()),
                cartItem.getUnitPrice(),
                cartItem.getQuantity(),
                cartItem.getSubTotal()
        );
    }

    private String resolveCurrentKeycloakId() {
        UserContext context = UserContext.get();
        if (context == null || context.getKeycloakId() == null || context.getKeycloakId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được người dùng hiện tại");
        }
        return context.getKeycloakId().trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long parseProductId(String productId) {
        try {
            return Long.parseLong(productId.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId không hợp lệ", ex);
        }
    }

    private String toSelectedOptionsSnapshot(List<CartItemOptionSelectionRequest> selectedOptions) {
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(selectedOptions);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "selectedOptions không hợp lệ", ex);
        }
    }

    private List<CartItemOptionSelectionResponse> fromSelectedOptionsSnapshot(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(snapshot, new TypeReference<List<CartItemOptionSelectionResponse>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không đọc được selectedOptions snapshot", ex);
        }
    }
}



