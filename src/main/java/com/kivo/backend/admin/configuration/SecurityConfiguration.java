package com.kivo.backend.admin.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties({SecurityProperties.class, CorsProperties.class})
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      SecurityProperties properties,
      ApiAuthenticationEntryPoint authenticationEntryPoint,
      ApiAccessDeniedHandler accessDeniedHandler,
      UserDetailsService adminUserDetailsService)
      throws Exception {
    http.authorizeHttpRequests(
        authorization ->
            authorization
                .requestMatchers("/api/v1/admin/**")
                .hasRole("STAFF")
                .anyRequest()
                .permitAll());
    http.headers(
        headers ->
            headers.httpStrictTransportSecurity(
                hsts ->
                    hsts.maxAgeInSeconds(properties.hsts().maxAge().toSeconds())
                        .includeSubDomains(properties.hsts().includeSubdomains())
                        .preload(properties.hsts().preload())));
    http.sessionManagement(
        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.exceptionHandling(
        exceptions ->
            exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));
    http.httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint));
    http.cors(Customizer.withDefaults());
    http.csrf(csrf -> csrf.disable());
    http.userDetailsService(adminUserDetailsService);
    if (properties.requireHttps()) {
      http.redirectToHttps(Customizer.withDefaults());
    }
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService adminUserDetailsService(SecurityProperties properties) {
    var admin = properties.admin();
    var user =
        User.withUsername(admin.username()).password(admin.passwordHash()).roles("STAFF").build();
    return new InMemoryUserDetailsManager(user);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.allowedOrigins());
    configuration.setAllowedMethods(properties.allowedMethods());
    configuration.setAllowedHeaders(properties.allowedHeaders());
    configuration.setExposedHeaders(properties.exposedHeaders());
    configuration.setAllowCredentials(properties.allowCredentials());
    configuration.setMaxAge(Duration.ofHours(1));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    source.registerCorsConfiguration("/r/**", configuration);
    return source;
  }
}
