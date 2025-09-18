package com.example.userservice.dto;

import lombok.Data;
import java.util.Date;

import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class UserDto {
    private String email;
    private String name;
    private String pwd;
    private String userId;
    private Date createdAt;

    private String encryptedPwd;

}

