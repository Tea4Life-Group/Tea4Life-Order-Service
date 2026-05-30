package tea4life.order_service.driver.service;

import tea4life.order_service.dto.request.driver.UpsertDriverRequest;
import tea4life.order_service.dto.response.driver.DriverResponse;

import java.util.List;

public interface DriverService {

    List<DriverResponse> findAllDrivers();

    DriverResponse findDriverById(Long id);

    DriverResponse createDriver(UpsertDriverRequest request);

    DriverResponse updateDriver(Long id, UpsertDriverRequest request);

    void deleteDriver(Long id);
}


