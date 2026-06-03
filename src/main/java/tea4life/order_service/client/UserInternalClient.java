package tea4life.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tea4life.order_service.dto.base.ApiResponse;

@FeignClient(
        name = "TEA4LIFE-USER-SERVICE",
        url = "${service.url.user-internal:}",
        path = "/internal/users"
)
public interface UserInternalClient {

    @PostMapping("/{keycloakId}/role/{roleName}")
    ApiResponse<Void> assignRole(
            @PathVariable("keycloakId") String keycloakId,
            @PathVariable("roleName") String roleName
    );

    @PostMapping("/{keycloakId}/driver-role/member")
    ApiResponse<Void> downgradeDriverRoleToMember(@PathVariable("keycloakId") String keycloakId);
}
