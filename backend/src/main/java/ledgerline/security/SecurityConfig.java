package ledgerline.security;

import jakarta.servlet.http.HttpServletResponse;
import ledgerline.audit.AuditAction;
import ledgerline.audit.AuditOutcome;
import ledgerline.audit.AuditService;
import ledgerline.auth.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder(@Value("${ledgerline.security.bcrypt-strength:12}") int strength) {
        return new BCryptPasswordEncoder(strength);
    }

    /** We do our own token login, so Spring's default generated-password user must not exist. */
    @Bean
    UserDetailsService noDefaultUsers() {
        return username -> {
            throw new UsernameNotFoundException("not used");
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SessionService sessionService, AuditService audit)
            throws Exception {
        http
                // CSRF protection is for cookie sessions. We use a bearer token header, which browsers never add automatically.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/health", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")   // server-side role check
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                writeJson(response, 401, "UNAUTHENTICATED", "Authentication required"))
                        .accessDeniedHandler((request, response, e) -> {
                            AuthenticatedUser who = currentUser();
                            audit.recordIndependent(AuditAction.ACCESS_DENIED, AuditOutcome.DENIED,
                                    who == null ? null : who.id(), who == null ? null : who.username(),
                                    "ENDPOINT", request.getMethod() + " " + request.getRequestURI(),
                                    "Role not allowed");
                            writeJson(response, 403, "FORBIDDEN", "Access denied");
                        }))
                .addFilterBefore(new TokenAuthFilter(sessionService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${ledgerline.cors.allowed-origins:http://localhost:5173}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        config.setExposedHeaders(List.of("Idempotent-Replay"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static AuthenticatedUser currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthenticatedUser u ? u : null;
    }

    private static void writeJson(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
