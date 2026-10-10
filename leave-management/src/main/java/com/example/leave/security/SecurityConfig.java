package com.example.leave.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // enables @PreAuthorize on controller methods
public class SecurityConfig {

    private final JwtService jwtService;
    private final EmployeeUserDetailsService userDetailsService;
    private final JsonSecurityErrorHandler errorHandler;

    public SecurityConfig(JwtService jwtService,
                          EmployeeUserDetailsService userDetailsService,
                          JsonSecurityErrorHandler errorHandler) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.errorHandler = errorHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless token API: no cookies/session, so CSRF protection is not needed
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                // dev only: the H2 console page uses frames
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // built-in UI page (static/index.html) - data calls still need a JWT
                        .requestMatchers("/h2-console/**").permitAll() // only active when the h2 console is enabled
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        // The single-page web UI (src/main/resources/static/index.html). It only
                        // contains HTML/JS; every /api/** call it makes still requires the JWT.
                        .requestMatchers(HttpMethod.GET, "/", "/index.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
