package tea4life.order_service.dto.event;

import java.util.List;

public record OrderPlacedItemEvent(
        Long productId,
        Long categoryId,
        Integer quantity,
        List<Long> optionValueIds
) {
}
