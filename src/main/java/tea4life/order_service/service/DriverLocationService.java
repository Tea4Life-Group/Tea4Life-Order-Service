package tea4life.order_service.service;

import tea4life.order_service.dto.response.driver.DriverLocationResponse;

import java.util.List;

public interface DriverLocationService {

    DriverLocationResponse saveLocation(Long orderId, DriverLocationResponse payload);

    DriverLocationResponse getLatest(Long orderId);

    List<DriverLocationResponse> getHistory(Long orderId);

}
