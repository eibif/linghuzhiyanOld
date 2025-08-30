package org.linghu.mybackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.linghu.mybackend.config.SecurityConfig;
import org.linghu.mybackend.dto.*;
import org.linghu.mybackend.security.JwtAccessDeniedHandler;
import org.linghu.mybackend.security.JwtAuthenticationEntryPoint;
import org.linghu.mybackend.security.JwtTokenUtil;
import org.linghu.mybackend.security.UserDetailsServiceImpl;
import org.linghu.mybackend.service.LoginLogService;
import org.linghu.mybackend.service.UserService;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
@WebMvcTest(UserController.class)
////@Import(SecurityConfig.class)
@ActiveProfiles("test")
//@Import(SecurityConfig.class)

class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private LoginLogService loginLogService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private UserRegistrationDTO registrationDTO;
    private UserDTO userDTO;
    private ProfileUpdateDTO profileUpdateDTO;
    private SetRoleRequestDTO setRoleRequestDTO;
    private LoginRequestDTO loginRequestDTO;
    private LoginResponseDTO loginResponseDTO;

    @BeforeEach
    void setUp() {
        // 模拟用户注册请求
        registrationDTO = new UserRegistrationDTO();
        registrationDTO.setUsername("testuser");
        registrationDTO.setEmail("test@example.com");
        registrationDTO.setPassword("password123");

        // 模拟用户登录请求
        loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setUsername("testuser");
        loginRequestDTO.setPassword("password123");

        // 模拟返回的用户DTO
        userDTO = new UserDTO();
        userDTO.setId("u123");
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");

        // 模拟登录响应DTO
        loginResponseDTO = new LoginResponseDTO();
        loginResponseDTO.setToken("test-jwt-token");
        loginResponseDTO.setUser(userDTO);

        // 模拟个人资料更新DTO
        profileUpdateDTO = new ProfileUpdateDTO();

        // 模拟设置角色请求DTO
        setRoleRequestDTO = new SetRoleRequestDTO();
        setRoleRequestDTO.setUserId("u123");
        setRoleRequestDTO.setRoleId("ROLE_STUDENT");
    }

    @Test
    void login_Failure_InvalidCredentials() throws Exception {
        LoginRequestDTO invalidLoginDTO = new LoginRequestDTO();
        invalidLoginDTO.setUsername("wronguser");
        invalidLoginDTO.setPassword("wrongpass");

        when(userService.login(any(LoginRequestDTO.class)))
                .thenThrow(new RuntimeException("用户名或密码错误"));

        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLoginDTO)))
                .andExpect(status().is4xxClientError());
    }

    // ===== 删除用户测试 =====
    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(anyString(), anyString());

        mockMvc.perform(delete("/api/users/delete/u123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void deleteUser_Failure_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/users/delete/u123")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ===== 获取个人资料测试 =====
    @Test
    @WithMockUser(username = "testuser")
    void getProfile_Success() throws Exception {
        when(userService.getUserByUsername("testuser")).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void getProfile_Failure_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    // ===== 更新个人资料测试 =====
    @Test
    @WithMockUser(username = "testuser")
    void updateProfile_Success() throws Exception {
        when(userService.updateUserProfile(anyString(), any(ProfileUpdateDTO.class))).thenReturn(userDTO);

        mockMvc.perform(put("/api/users/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileUpdateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void updateProfile_Failure_Unauthorized() throws Exception {
        mockMvc.perform(put("/api/users/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileUpdateDTO)))
                .andExpect(status().isUnauthorized());
    }

    // ===== 修改密码测试 =====
    @Test
    @WithMockUser(username = "testuser")
    void changePassword_Success() throws Exception {
        doNothing().when(userService).changePassword(anyString(), anyString(), anyString());

        mockMvc.perform(put("/api/users/password")
                        .with(csrf())
                        .param("oldPassword", "oldpass")
                        .param("newPassword", "newpass"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_Failure_ServiceException() throws Exception {
        doThrow(new RuntimeException("密码错误")).when(userService)
                .changePassword(anyString(), anyString(), anyString());

        mockMvc.perform(put("/api/users/password")
                        .with(csrf())
                        .param("oldPassword", "wrongpass")
                        .param("newPassword", "newpass"))
                .andExpect(status().is5xxServerError());
    }

    // ===== 获取指定用户测试 =====
    @Test
    @WithMockUser(roles = "ADMIN")
    void getUser_Success() throws Exception {
        when(userService.getUserById("u123")).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/u123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("u123"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getUser_Failure_Forbidden() throws Exception {
        mockMvc.perform(get("/api/users/u123"))
                .andExpect(status().isForbidden());
    }

    // ===== 分页查询用户测试 =====
    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_Success() throws Exception {
        List<UserDTO> users = Arrays.asList(userDTO);
        Page<UserDTO> page = new PageImpl<>(users);
        when(userService.listUsers(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/users")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void listUsers_Failure_Forbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    // ===== 上传头像测试 =====
    @Test
    @WithMockUser(username = "testuser")
    void uploadAvatar_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "content".getBytes());
        Map<String, String> avatarInfo = Map.of("path", "/avatars/avatar.jpg", "url", "http://example.com/avatar.jpg");
        
        when(userService.updateUserAvatar(anyString(), any(MultipartFile.class))).thenReturn(avatarInfo);

        mockMvc.perform(multipart("/api/users/avatar")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.path").value("/avatars/avatar.jpg"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void uploadAvatar_Failure_EmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/api/users/avatar")
                        .file(emptyFile)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请选择要上传的文件"));
    }

    // ===== 获取头像URL测试 =====
    @Test
    @WithMockUser(username = "u123", roles = {"USER"})
    void getAvatarUrl_Success() throws Exception {
        when(userService.getUserAvatarUrl("u123")).thenReturn("http://example.com/avatar.jpg");

        mockMvc.perform(get("/api/users/avatar/u123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("http://example.com/avatar.jpg"));
    }

    // ===== 设置用户角色测试 =====
    @Test
    @WithMockUser(roles = "ADMIN")
    void setRole_Success() throws Exception {
        doNothing().when(userService).setUserRole(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/users/setrole")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setRoleRequestDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void setRole_Failure_Forbidden() throws Exception {
        mockMvc.perform(post("/api/users/setrole")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setRoleRequestDTO)))
                .andExpect(status().isForbidden());
    }
}
