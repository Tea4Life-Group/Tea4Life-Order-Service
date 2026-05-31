package tea4life.order_service.delivery.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tea4life.order_service.dto.response.driver.DriverLocationResponse;
import tea4life.order_service.model.driver.DriverLocation;
import tea4life.order_service.model.order.Order;
import tea4life.order_service.delivery.repository.DriverLocationRepository;
import tea4life.order_service.order.repository.OrderRepository;
import tea4life.order_service.driver.repository.DriverRepository;
import tea4life.order_service.delivery.service.DriverLocationService;
import tea4life.order_service.context.UserContext;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class DriverLocationServiceImpl implements DriverLocationService {

    DriverLocationRepository driverLocationRepository;
    OrderRepository orderRepository;
    DriverRepository driverRepository;

    @Override
    public DriverLocationResponse saveLocation(Long orderId, DriverLocationResponse payload) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Order với ID: " + orderId));

        String currentKeycloakId = resolveCurrentKeycloakId();

        // Ensure driver assigned to this order
        if (order.getDriverKeycloakId() == null || !order.getDriverKeycloakId().equals(currentKeycloakId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không được phép cập nhật vị trí cho đơn này");
        }

        DriverLocation loc = new DriverLocation();
        loc.setOrder(order);
        loc.setDriverKeycloakId(currentKeycloakId);
        loc.setLatitude(payload.getLatitude());
        loc.setLongitude(payload.getLongitude());
        loc.setAccuracy(payload.getAccuracy());

        DriverLocation saved = driverLocationRepository.save(loc);

        // Map to response
        DriverLocationResponse res = new DriverLocationResponse(
                saved.getId() == null ? null : saved.getId().toString(),
                order.getId() == null ? null : order.getId().toString(),
                saved.getDriverKeycloakId(),
                saved.getLatitude(),
                saved.getLongitude(),
                saved.getAccuracy(),
                saved.getCreatedAt()
        );

        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public DriverLocationResponse getLatest(Long orderId) {
        DriverLocation latest = driverLocationRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElse(null);
        if (latest == null) return null;
        return new DriverLocationResponse(
                latest.getId() == null ? null : latest.getId().toString(),
                latest.getOrder() == null || latest.getOrder().getId() == null ? null : latest.getOrder().getId().toString(),
                latest.getDriverKeycloakId(),
                latest.getLatitude(),
                latest.getLongitude(),
                latest.getAccuracy(),
                latest.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverLocationResponse> getHistory(Long orderId) {
        return driverLocationRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(saved -> new DriverLocationResponse(
                        saved.getId() == null ? null : saved.getId().toString(),
                        saved.getOrder() == null || saved.getOrder().getId() == null ? null : saved.getOrder().getId().toString(),
                        saved.getDriverKeycloakId(),
                        saved.getLatitude(),
                        saved.getLongitude(),
                        saved.getAccuracy(),
                        saved.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private String resolveCurrentKeycloakId() {
        UserContext context = UserContext.get();
        if (context == null || context.getKeycloakId() == null || context.getKeycloakId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được người dùng hiện tại");
        }
        String keycloakId = context.getKeycloakId().trim();
        if (driverRepository.findByKeycloakId(keycloakId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải là tài xế hợp lệ");
        }
        return keycloakId;
    }
}



