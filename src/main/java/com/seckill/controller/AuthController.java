package com.seckill.controller;

import com.seckill.dto.Result;
import com.seckill.dto.UserInfoDTO;
import com.seckill.dto.UserLoginDTO;
import com.seckill.dto.UserRegisterDTO;
import com.seckill.service.UserService;
import com.seckill.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<Result<UserInfoDTO>> register(@Valid @RequestBody UserRegisterDTO dto) {
        log.info("用户注册请求: username={}", dto.getUsername());
        try {
            UserInfoDTO result = userService.register(dto);
            return ResponseEntity.ok(Result.success("注册成功", result));
        } catch (Exception e) {
            log.error("注册失败", e);
            return ResponseEntity.ok(Result.fail(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Result<UserInfoDTO>> login(@Valid @RequestBody UserLoginDTO dto) {
        log.info("用户登录请求: username={}", dto.getUsername());
        try {
            UserInfoDTO result = userService.login(dto);
            return ResponseEntity.ok(Result.success("登录成功", result));
        } catch (Exception e) {
            log.error("登录失败", e);
            return ResponseEntity.ok(Result.fail(e.getMessage()));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Result<UserInfoDTO>> getUserInfo(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.ok(Result.fail("未登录或登录已过期"));
        }

        Long userId = jwtUtil.getUserId(token);
        UserInfoDTO result = userService.getUserInfo(userId);

        if (result == null) {
            return ResponseEntity.ok(Result.fail("用户不存在"));
        }

        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/check-username")
    public ResponseEntity<Result<Boolean>> checkUsername(@RequestParam String username) {
        boolean exists = userService.checkUsernameExists(username);
        return ResponseEntity.ok(Result.success(exists));
    }
}
