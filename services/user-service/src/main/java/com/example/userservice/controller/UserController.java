package com.example.userservice.controller;

import com.example.userservice.dto.UserDto;
import com.example.userservice.jpa.UserEntity;
import com.example.userservice.service.UserService;
import com.example.userservice.vo.Greeting;
import com.example.userservice.vo.RequestUser;
import com.example.userservice.vo.ResponseUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/user-service")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final Environment env;
    private final Greeting greeting;
    private final UserService userService;
    private final ModelMapper modelMapper;

    /**
     * 서비스 헬스체크 API
     * - 현재 User Service의 상태 및 주요 환경 변수 값을 확인
     */
    @GetMapping("/health-check")
    public String status() {
        return String.format("It's Working in User Service"
                + ", port(local.server.port)=" + env.getProperty("local.server.port")
                + ", port(server.port)=" + env.getProperty("server.port")
                + ", welcome message =" + env.getProperty("greeting.message")
                + ", token secret key=" + env.getProperty("token.secret")
                + ", token expiration time=" + env.getProperty("token.expiration-time"));
    }


    @GetMapping("/welcome")
    public String welcome(HttpServletRequest request) {
        log.info("users.welcome ip: {}, {}, {}, {}", request.getRemoteAddr()
                , request.getRemoteHost(), request.getRequestURI(), request.getRequestURL());

        return greeting.getMessage();
    }

    /**
     * 사용자 생성 API
     * - RequestUser → UserDto 변환 (입력 DTO)
     * - userService.createUser(userDto) 호출
     * - 결과를 ResponseUser로 변환하여 반환 (출력 DTO)
     */
    @PostMapping("/signup")
    public ResponseEntity<ResponseUser> createUser(@RequestBody RequestUser user) {

        UserDto userDto = modelMapper.map(user, UserDto.class);
        userService.createUser(userDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(modelMapper.map(userDto, ResponseUser.class));
    }

    /**
     * 사용자 전체 조회 API
     * - DB에 저장된 모든 UserEntity 조회
     * - 각 UserEntity를 ResponseUser로 변환하여 리스트 반환
     */
    @GetMapping("/users")
    public ResponseEntity<List<ResponseUser>> getUsers() {
        Iterable<UserEntity> userList = userService.getUserByAll();

        List<ResponseUser> result = new ArrayList<>();
        userList.forEach(v -> {
            result.add(modelMapper.map(v, ResponseUser.class));
        });

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /**
     * 특정 사용자 조회 API
     * - userId로 사용자 검색
     * - UserDto → ResponseUser 변환 후 반환
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ResponseUser> getUser(@PathVariable("userId") String userId) {
        UserDto userDto = userService.getUserByUserId(userId);

        return ResponseEntity.status(HttpStatus.OK).body(modelMapper.map(userDto, ResponseUser.class));
    }
}
