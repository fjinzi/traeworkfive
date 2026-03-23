package com.seckill.service;

import com.seckill.dto.UserInfoDTO;
import com.seckill.dto.UserLoginDTO;
import com.seckill.dto.UserRegisterDTO;

public interface UserService {
    UserInfoDTO register(UserRegisterDTO dto);
    UserInfoDTO login(UserLoginDTO dto);
    UserInfoDTO getUserInfo(Long userId);
    boolean checkUsernameExists(String username);
}
