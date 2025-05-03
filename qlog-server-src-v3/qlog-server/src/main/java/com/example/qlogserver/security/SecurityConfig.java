package com.example.qlogserver.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration for the QLog Server.
 * Configures Basic Authentication for agent endpoints and allows access to UI/other APIs.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Configures security rules specifically for the agent API endpoint.
     * Requires Basic Authentication and the 'AGENT' role.
     *
     * @param http HttpSecurity configuration object.
     * @return The configured SecurityFilterChain.
     * @throws Exception If configuration fails.
     */
    @Bean
    @Order(1) // Higher priority for agent API rules
    public SecurityFilterChain agentApiSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring SecurityFilterChain for Agent API (/api/agent/**)");
        http
            // Apply this filter chain only to /api/agent/** paths
            .securityMatcher(new AntPathRequestMatcher("/api/agent/**"))
            .authorizeHttpRequests(authorize -> authorize
                // Require AGENT role for all requests to /api/agent/**
                .anyRequest().hasRole("AGENT")
            )
            // Enable Basic Authentication for this filter chain
            .httpBasic(Customizer.withDefaults())
            // Disable CSRF as this is an API endpoint likely not called from browsers directly by users
            .csrf(AbstractHttpConfigurer::disable)
            // Use stateless sessions for the API
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    /**
     * Configures security rules for all other endpoints (UI, web APIs, static resources).
     * Allows all access by default, but can be customized.
     *
     * @param http HttpSecurity configuration object.
     * @return The configured SecurityFilterChain.
     * @throws Exception If configuration fails.
     */
    @Bean
    @Order(2) // Lower priority for general rules
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring SecurityFilterChain for UI and other endpoints.");
        http
            .authorizeHttpRequests(authorize -> authorize
                // Allow access to H2 console if enabled (adjust path if needed)
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                // Allow access to static resources (CSS, JS)
                .requestMatchers(new AntPathRequestMatcher("/**.html"), new AntPathRequestMatcher("/static/**"), new AntPathRequestMatcher("/css/**"), new AntPathRequestMatcher("/js/**")).permitAll()
                // Allow access to the main UI page and other API endpoints by default
                // Adjust this if UI/reporting needs specific authentication/authorization
                .anyRequest().permitAll() // Or .authenticated() if login is desired for UI
            )
            // IMPORTANT: If H2 console is enabled, disable frame options
            .headers(headers -> headers.frameOptions(Customizer.withDefaults()).disable()) // Allow H2 console in iframe
            // Disable CSRF for simplicity, enable and configure properly if using form login for UI
            .csrf(AbstractHttpConfigurer::disable);
            // Configure form login if UI authentication is needed
            // .formLogin(Customizer.withDefaults());

        return http.build();
    }

    // Note: User details (username, password, roles) are typically configured
    // in application.properties (spring.security.user.*) for simple cases,
    // or via a UserDetailsService bean for more complex scenarios (e.g., DB lookup).
    // The properties configuration is sufficient for the agent user.
}

