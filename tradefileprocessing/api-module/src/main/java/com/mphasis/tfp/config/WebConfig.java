package com.mphasis.tfp.config;

import com.mphasis.tfp.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;

    // ── CORS Configuration ─────────────────────────────────────────────
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")  // Angular default port
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }

    // ── JWT Interceptor Configuration ──────────────────────────────────
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor(jwtUtil))
                .addPathPatterns("/Files/**")
                .excludePathPatterns(
                        "/auth/register",
                        "/auth/login"
                );
    }

    // ── JWT Interceptor ────────────────────────────────────────────────
    @Slf4j
    @RequiredArgsConstructor
    static class JwtInterceptor implements HandlerInterceptor {

        private final JwtUtil jwtUtil;

        @Override
        public boolean preHandle(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Object handler) throws Exception {

            // Handle Angular preflight OPTIONS request
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
                return true;
            }

            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for: {}",
                        request.getRequestURI());
                sendUnauthorized(response,
                        "Missing or invalid Authorization header. Please login first.");
                return false;
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid or expired JWT token for: {}", request.getRequestURI());
                sendUnauthorized(response,
                        "Invalid or expired token. Please login again.");
                return false;
            }

            String username = jwtUtil.extractUsername(token);
            log.debug("Authorized request from user: {} to: {}",
                    username, request.getRequestURI());
            request.setAttribute("loggedInUser", username);
            return true;
        }

        private void sendUnauthorized(HttpServletResponse response,
                                      String message) throws Exception {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":\"ERROR\",\"code\":\"401\",\"message\":\""
                            + message + "\",\"data\":null}"
            );
        }
    }
}