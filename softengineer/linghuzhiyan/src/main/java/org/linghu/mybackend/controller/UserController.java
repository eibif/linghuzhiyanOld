package org.linghu.mybackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.linghu.mybackend.dto.LoginRequestDTO;
import org.linghu.mybackend.dto.LoginResponseDTO;
import org.linghu.mybackend.dto.PageResult;
import org.linghu.mybackend.dto.ProfileUpdateDTO;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.dto.SetRoleRequestDTO;
import org.linghu.mybackend.dto.UserDTO;
import org.linghu.mybackend.dto.UserRegistrationDTO;
import org.linghu.mybackend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户API控制器
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户相关API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public Result<UserDTO> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        UserDTO userDTO = userService.registerUser(registrationDTO);
        return Result.success(userDTO);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户以指定身份登录并获取令牌，可选择指定角色进行身份验证")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        LoginResponseDTO response = userService.login(loginRequestDTO);
        return Result.success(response);
    }

    @DeleteMapping("/delete/{userId}")
    @Operation(summary = "删除用户", description = "只有管理员可以删除用户")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Result<Void> deleteUser(@PathVariable String userId,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteUser(userId, userDetails.getUsername());
        return Result.success();
    }

    @GetMapping("/profile")
    @Operation(summary = "获取个人资料", description = "获取当前登录用户的个人资料")
    public Result<UserDTO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserDTO userDTO = userService.getUserByUsername(userDetails.getUsername());
        return Result.success(userDTO);
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料", description = "更新当前登录用户的个人资料，只允许修改头像和个人信息")
    public Result<UserDTO> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileUpdateDTO profileUpdateDTO) {
        return Result.success(userService.updateUserProfile(userDetails.getUsername(), profileUpdateDTO));
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    public Result<Void> changePassword(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        userService.changePassword(userDetails.getUsername(), oldPassword, newPassword);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取指定用户", description = "根据ID获取用户信息")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_TEACHER','ROLE_ASSISTANT')")
    public Result<UserDTO> getUser(@PathVariable String id) {
        UserDTO userDTO = userService.getUserById(id);
        return Result.success(userDTO);
    }

    @GetMapping
    @Operation(summary = "分页查询用户", description = "分页查询用户列表")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_TEACHER','ROLE_ASSISTANT')")
    public Result<PageResult<UserDTO>> listUsers(@RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<UserDTO> page = userService.listUsers(pageNum, pageSize);

        PageResult<UserDTO> pageResult = new PageResult<>();
        pageResult.setList(page.getContent());
        pageResult.setTotal(page.getTotalElements());
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);

        return Result.success(pageResult);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传头像", description = "上传用户头像，图片将存储到MinIO")
    public Result<Map<String, String>> uploadAvatar(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.failure(400, "请选择要上传的文件");
        }
        
        Map<String, String> avatarInfo = userService.updateUserAvatar(userDetails.getUsername(), file);
        return Result.success(avatarInfo);
    }    
    
    @GetMapping("/avatar/{userId}")
    @Operation(summary = "获取用户头像URL", description = "获取指定用户的头像访问URL")
    public Result<String> getAvatarUrl(@PathVariable String userId) {
        String avatarUrl = userService.getUserAvatarUrl(userId);
        return Result.success(avatarUrl);
    }    
    
    @PostMapping("/setrole")
    @Operation(summary = "设置用户角色", description = "具有更高权限的用户可以为其他用户分配同级或更低级权限")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_TEACHER','ROLE_ASSISTANT')")
    public Result<Void> setRole(@Valid @RequestBody SetRoleRequestDTO request,
                               @AuthenticationPrincipal UserDetails userDetails) {
        userService.setUserRole(request.getUserId(), request.getRoleId(), userDetails.getUsername());
        return Result.success();
    }
    
}
