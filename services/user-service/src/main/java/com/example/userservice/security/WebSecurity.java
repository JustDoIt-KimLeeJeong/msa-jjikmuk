package com.example.userservice.security;

import com.example.userservice.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
public class WebSecurity {
    private final UserService userService;
    private final Environment env;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public WebSecurity(Environment env, UserService userService, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.env = env;
        this.userService = userService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userService).passwordEncoder(bCryptPasswordEncoder);

        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

        http.csrf( (csrf) -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/health-check/**").permitAll()
                        .requestMatchers("/**").access(
                                new WebExpressionAuthorizationManager(getIpAccessExpression()))
                        .anyRequest().authenticated()
                )
                .authenticationManager(authenticationManager)
                .addFilter(getAuthenticationFilter(authenticationManager))
                .headers((headers) -> headers
                        .frameOptions((frameOptions) -> frameOptions.sameOrigin()));

        return http.build();
    }

    private String getIpAccessExpression() {
        String[] ips = env.getProperty("gateway.ip", String[].class);
        if (ips == null || ips.length == 0) {
            return "hasIpAddress('127.0.0.1') or hasIpAddress('::1')";
        }
        return Stream.of(ips)
                .map(ip -> "hasIpAddress('" + ip + "')")
                .collect(Collectors.joining(" or "));
    }


    private AuthenticationFilter getAuthenticationFilter(AuthenticationManager authenticationManager) throws Exception {
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(userService, env, authenticationManager);
        authenticationFilter.setAuthenticationManager(authenticationManager);
        authenticationFilter.setFilterProcessesUrl("/user-service/login");

        return authenticationFilter;
    }
}
