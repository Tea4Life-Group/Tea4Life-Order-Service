package tea4life.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tea4life.order_service.dto.base.ApiResponse;

@FeignClient(
        name = "TEA4LIFE-USER-SERVICE",
        path = "/internal/users"
)
public interface UserInternalClient {

    @PatchMapping("/{keycloakId}/role/{roleName}")
    ApiResponse<Void> assignRole(
            @PathVariable("keycloakId") String keycloakId,
            @PathVariable("roleName") String roleName
    );
}
