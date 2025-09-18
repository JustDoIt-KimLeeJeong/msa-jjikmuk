package com.example.userservice.security;

import com.example.userservice.dto.UserDto;
import com.example.userservice.service.UserService;
import com.example.userservice.vo.RequestLogin;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;


import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
/**
 * 로그인 엔드포인트(기본: POST /login)에서
 *  - 요청 바디(JSON) → {email, password} 파싱
 *  - AuthenticationManager에 위임하여 인증 수행
 *  - 성공 시 JWT 발급 후 Authorization 헤더로 내려줌
 */
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final UserService userService;
    private final Environment env;

    public AuthenticationFilter(UserService userService, Environment env,
                                AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.env = env;
        super.setAuthenticationManager(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {

        try {
            RequestLogin creds = new ObjectMapper().readValue(req.getInputStream(), RequestLogin.class);

            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            creds.getEmail(),
                            creds.getPassword(),
                            new ArrayList<>()
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication authResult)
            throws IOException, ServletException {
        String userEmail = ((User)authResult.getPrincipal()).getUsername(); // email
        UserDto userDetails = userService.getUserDetailsByEmail(userEmail);

        byte[] secretKeyBytes = env.getProperty("token.secret") // application.yml
                .getBytes(StandardCharsets.UTF_8);

        SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyBytes);

        Instant now = Instant.now();

        String token = Jwts.builder()
                .subject(userDetails.getUserId()) // UUID
                .expiration(Date.from(now.plusMillis(Long.parseLong(
                        Objects.requireNonNull(env.getProperty("token.expiration-time")))))) // 만료 시간
                .issuedAt(Date.from(now)) // 발행시간
                .signWith(secretKey)
                .compact();

        res.addHeader("token", token);
        res.addHeader("userId", userDetails.getUserId());
    }
}
