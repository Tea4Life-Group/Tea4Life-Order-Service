package tea4life.order_service.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tea4life.order_service.context.UserContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Admin 2/7/2026
 *
 **/
@Component
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain
    ) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        String email = httpRequest.getHeader("X-User-Email");
        String userKeycloakId = httpRequest.getHeader("X-User-KeycloakId");
        String role = normalizeRole(httpRequest.getHeader("X-User-Role"));
        String authoritiesRaw = httpRequest.getHeader("X-User-Authorities");

        /**
         * =============================================
         * ĐỒNG BỘ HÓA SPRING SECURITY CONTEXT
         * Chuyển đổi chuỗi Permissions từ Header thành danh sách SimpleGrantedAuthority
         * để kích hoạt các cơ chế bảo mật như @PreAuthorize trên Controller.
         * =============================================
         **/

        List<SimpleGrantedAuthority> authorities = Collections.emptyList();

        if (authoritiesRaw != null && !authoritiesRaw.isBlank()) {
            authorities = Arrays.stream(authoritiesRaw.split(","))
                    .filter(auth -> !auth.isBlank())
                    .map(SimpleGrantedAuthority::new)
                    .toList();
        }

        if (!role.isBlank()) {
            authorities = Stream.concat(
                            authorities.stream(),
                            Stream.of(new SimpleGrantedAuthority(role))
                    )
                    .distinct()
                    .toList();
        }

        var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        /**
         * =============================================
         * THIẾT LẬP CONTEXT ĐỊNH DANH NGƯỜI DÙNG
         * Lưu trữ thông tin định danh vào ThreadLocal giúp tầng Service dễ dàng truy cập
         * mà không cần thông qua lớp bảo mật phức tạp của Spring.
         * =============================================
         **/

        UserContext context = UserContext.builder()
                .email(email)
                .keycloakId(userKeycloakId)
                .role(role)
                .build();

        UserContext.set(context);

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }

    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.replaceFirst("^ROLE_", "").toUpperCase();
    }
}
