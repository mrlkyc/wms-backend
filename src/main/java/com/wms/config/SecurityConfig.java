package com.wms.config;

import com.wms.security.CustomAuthenticationSuccessHandler;
import com.wms.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ================= CORS & CSRF =================
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                // ================= AUTHORIZATION =================
                .authorizeHttpRequests(auth -> auth

                        // 🔓 AUTH & SWAGGER & ACTUATOR
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()

                        // 🔓 THYMELEAF SAYFALARI & STATICS
                        .requestMatchers(
                                        "/v3/api-docs",
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                "/login",
                                "/register",
                                "/products",
                                "/inventory",
                                "/locations",
                                "/orders",
                                "/purchase-orders",
                                "/admin",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**"
                        ).permitAll()

                        // ================= REST API ROLE KURALLARI =================

                        // PRODUCTS
                        .requestMatchers(HttpMethod.GET, "/api/products/**")
                        .hasAnyRole("ADMIN", "MANAGER", "WORKER")
                        .requestMatchers(HttpMethod.POST, "/api/products/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // WAREHOUSES
                        .requestMatchers(HttpMethod.GET, "/api/warehouses/**")
                        .hasAnyRole("ADMIN", "MANAGER", "WORKER")
                        .requestMatchers(HttpMethod.POST, "/api/warehouses/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/warehouses/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/warehouses/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // LOCATIONS
                        .requestMatchers(HttpMethod.GET, "/api/locations/**")
                        .hasAnyRole("ADMIN", "MANAGER", "WORKER")
                        .requestMatchers(HttpMethod.POST, "/api/locations/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/locations/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/locations/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // INVENTORY
                        .requestMatchers(HttpMethod.GET, "/api/inventory/**")
                        .hasAnyRole("ADMIN", "MANAGER", "WORKER")
                        .requestMatchers(HttpMethod.POST, "/api/inventory/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/inventory/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/inventory/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // STOCK
                        .requestMatchers("/api/stock/**")
                        .hasAnyRole("ADMIN", "MANAGER", "WORKER")

                        // PURCHASE ORDERS
                        .requestMatchers("/api/purchase-orders/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // ORDERS
                        .requestMatchers("/api/orders/**")
                        .hasAnyRole("ADMIN", "MANAGER", "WORKER")

                        // SUPPLIERS
                        .requestMatchers(HttpMethod.GET, "/api/suppliers/**")
                        .hasAnyRole("ADMIN", "MANAGER", "WORKER")
                        .requestMatchers(HttpMethod.POST, "/api/suppliers/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/suppliers/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/suppliers/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // REPORTS
                        .requestMatchers("/api/reports/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // ADMIN API
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // 🔒 DİĞER HER ŞEY
                        .anyRequest().authenticated()
                )

                // ================= SESSION =================
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ================= AUTH PROVIDER =================
                .authenticationProvider(authenticationProvider())

                // ================= JWT FILTER =================
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // ================= FORM LOGIN (THYMELEAF) =================
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .permitAll()
                );

        return http.build();
    }

    // ================= AUTH BEANS =================

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ================= CORS =================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
