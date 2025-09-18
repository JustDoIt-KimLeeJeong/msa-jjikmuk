package com.example.userservice.service;

import com.example.userservice.dto.UserDto;
import com.example.userservice.jpa.UserEntity;
import com.example.userservice.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final Environment env;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;


    /**
     * 스프링 시큐리티에서 사용자 인증 시 호출되는 메소드
     * username(email)으로 DB 조회 → UserDetails 반환
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(username + ": not found"));

        return new User(
                userEntity.getEmail(),
                userEntity.getEncryptedPwd(),
                true, true, true, true,
                Collections.emptyList()
        );
    }

    /**
     * 사용자 생성 (회원가입)
     * UserDto → UserEntity 변환 후 DB 저장
     */
    @Override
    @Transactional(readOnly = false) // false 로 해둘 시 snapshot과 비교해 업데이트 쿼리 및 insert 쿼리 날림
    public UserDto createUser(UserDto userDto) {
        // 중복 이메일 체크
        userRepository.findByEmail(userDto.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("Email already exists");
        });

        // 랜덤 UUID 생성하여 userId 설정
        userDto.setUserId(UUID.randomUUID().toString());

        // 랜덤 UUID 생성하여 userId 설정
        userDto.setUserId(UUID.randomUUID().toString());

        // DTO → Entity 매핑
        UserEntity entity = modelMapper.map(userDto, UserEntity.class);

        // 비밀번호 암호화 후 Entity에 저장
        entity.setEncryptedPwd(passwordEncoder.encode(userDto.getPwd()));

        // DB 저장
        userRepository.save(entity);

        return modelMapper.map(entity, UserDto.class);
    }

    /**
     * 전체 사용자 조회
     */
    @Override
    public Iterable<UserEntity> getUserByAll() {
        return userRepository.findAll();
    }

    /**
     * 이메일로 사용자 조회 후 DTO 반환
     */
    @Override
    public UserDto getUserDetailsByEmail(String email) {
        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email + " not found"));

        return modelMapper.map(userEntity, UserDto.class);
    }

    /**
     * userId(UUID)로 사용자 조회 후 DTO 반환
     */
    @Override
    public UserDto getUserByUserId(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return modelMapper.map(userEntity, UserDto.class);
    }


}
