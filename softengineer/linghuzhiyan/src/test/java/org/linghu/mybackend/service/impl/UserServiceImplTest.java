package org.linghu.mybackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.constants.SystemConstants;
import org.linghu.mybackend.domain.User;
import org.linghu.mybackend.dto.UserDTO;
import org.linghu.mybackend.dto.UserRegistrationDTO;
import org.linghu.mybackend.dto.ProfileUpdateDTO;
import org.linghu.mybackend.exception.UserException;
import org.linghu.mybackend.repository.UserRepository;
import org.linghu.mybackend.service.impl.UserServiceImpl;
import org.linghu.mybackend.util.MinioUtil;
import org.linghu.mybackend.repository.UserRoleRepository;
import org.linghu.mybackend.domain.UserRoleRelation;
import org.linghu.mybackend.domain.UserRoleId;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试类
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MinioUtil minioUtil;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserRegistrationDTO registrationDTO;
    private ProfileUpdateDTO profileUpdateDTO;
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        // 设置配置值
        ReflectionTestUtils.setField(userService, "avatarUrlExpiry", 3600);

        // 创建测试用户
        testUser = new User();
        testUser.setId("user-123");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded-password");
        testUser.setProfile("{}");
        testUser.setIsDeleted(false);
        testUser.setCreatedAt(new Date());
        testUser.setUpdatedAt(new Date());

        // 创建注册DTO
        registrationDTO = new UserRegistrationDTO();
        registrationDTO.setUsername("newuser");
        registrationDTO.setEmail("new@example.com");
        registrationDTO.setPassword("password123");

        // 创建资料更新DTO
        profileUpdateDTO = new ProfileUpdateDTO();

        // 创建模拟文件
        mockFile = mock(MultipartFile.class);
    }

    // ===== 用户注册测试 =====
    @Test
    void registerUser_Success() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRoleRepository.findByIdUserId("user-123")).thenReturn(new ArrayList<>());

        // When
        UserDTO result = userService.registerUser(registrationDTO);

        // Then
        assertNotNull(result);
        assertEquals("user-123", result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_Failure_UsernameExists() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // When & Then
        assertThrows(UserException.class, () -> userService.registerUser(registrationDTO));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_Failure_EmailExists() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        // When & Then
        assertThrows(UserException.class, () -> userService.registerUser(registrationDTO));
        verify(userRepository, never()).save(any(User.class));
    }

    // ===== 删除用户测试 =====
    @Test
    void deleteUser_Success() {
        // Given
        User userToUpdate = new User();
        userToUpdate.setId("user-123");
        userToUpdate.setUsername("testuser");
        userToUpdate.setIsDeleted(false);
        
        User adminUser = new User();
        adminUser.setId("admin-id");
        adminUser.setUsername("admin");
        
        // 创建管理员角色关联
        UserRoleRelation adminRole = new UserRoleRelation("admin-id", SystemConstants.ROLE_ADMIN);
        List<UserRoleRelation> adminRoles = Arrays.asList(adminRole);
        
        when(userRepository.findById("user-123")).thenReturn(Optional.of(userToUpdate));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRoleRepository.findByIdUserId("admin-id")).thenReturn(adminRoles);
        when(userRoleRepository.findByIdUserId("user-123")).thenReturn(new ArrayList<>());

        // When
        userService.deleteUser("user-123", "admin");

        // Then
        verify(userRepository).save(argThat(user -> user.getIsDeleted()));
    }

    @Test
    void deleteUser_Failure_UserNotFound() {
        // Given
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserException.class, () -> userService.deleteUser("user-123", "admin"));
    }

    // ===== 获取用户测试 =====
    @Test
    void getUserById_Success() {
        // Given
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRoleRepository.findByIdUserId("user-123")).thenReturn(new ArrayList<>());

        // When
        UserDTO result = userService.getUserById("user-123");

        // Then
        assertNotNull(result);
        assertEquals("user-123", result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserById_Failure_UserNotFound() {
        // Given
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserException.class, () -> userService.getUserById("nonexistent"));
    }

    @Test
    void getUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRoleRepository.findByIdUserId("user-123")).thenReturn(new ArrayList<>());

        // When
        UserDTO result = userService.getUserByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserByUsername_Failure_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserException.class, () -> userService.getUserByUsername("nonexistent"));
    }

    // ===== 修改密码测试 =====
    @Test
    void changePassword_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldpass", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("new-encoded-password");

        // When
        userService.changePassword("testuser", "oldpass", "newpass");

        // Then
        verify(userRepository).save(argThat(user -> "new-encoded-password".equals(user.getPassword())));
    }

    @Test
    void changePassword_Failure_WrongOldPassword() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpass", "encoded-password")).thenReturn(false);

        // When & Then
        assertThrows(UserException.class, () -> userService.changePassword("testuser", "wrongpass", "newpass"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_Failure_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserException.class, () -> userService.changePassword("nonexistent", "oldpass", "newpass"));
    }

    // ===== 分页查询用户测试 =====
    @Test
    void listUsers_Success() {
        // Given
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users);
        when(userRepository.findByIsDeletedFalse(any(Pageable.class))).thenReturn(userPage);
        when(userRoleRepository.findByIdUserId("user-123")).thenReturn(new ArrayList<>());

        // When
        Page<UserDTO> result = userService.listUsers(1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("testuser", result.getContent().get(0).getUsername());
    }

    // ===== 更新个人资料测试 =====
    @Test
    void updateUserProfile_Success() {
        // Given
        profileUpdateDTO.setAvatar("new-avatar.jpg");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRoleRepository.findByIdUserId("user-123")).thenReturn(new ArrayList<>());

        // When
        UserDTO result = userService.updateUserProfile("testuser", profileUpdateDTO);

        // Then
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserProfile_Failure_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserException.class, () -> userService.updateUserProfile("nonexistent", profileUpdateDTO));
    }

    // ===== 上传头像测试 =====
    @Test
    void updateUserAvatar_Success() throws Exception {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(minioUtil.uploadUserAvatar(mockFile, "user-123")).thenReturn("avatars/user-123.jpg");
        when(minioUtil.getAvatarPreviewUrl("avatars/user-123.jpg", 3600)).thenReturn("http://example.com/avatar.jpg");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        Map<String, String> result = userService.updateUserAvatar("testuser", mockFile);

        // Then
        assertNotNull(result);
        assertEquals("avatars/user-123.jpg", result.get("avatarPath"));
        assertEquals("http://example.com/avatar.jpg", result.get("avatarUrl"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserAvatar_Failure_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserException.class, () -> userService.updateUserAvatar("nonexistent", mockFile));
    }

    @Test
    void updateUserAvatar_Failure_UploadException() throws Exception {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(minioUtil.uploadUserAvatar(mockFile, "user-123")).thenThrow(new RuntimeException("Upload failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.updateUserAvatar("testuser", mockFile));
    }

    // ===== 获取头像URL测试 =====
    @Test
    void getUserAvatarUrl_Success_WithAvatar() throws Exception {
        // Given
        testUser.setAvatar("avatars/user-123.jpg");
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(minioUtil.getAvatarPreviewUrl("avatars/user-123.jpg", 3600)).thenReturn("http://example.com/avatar.jpg");

        // When
        String result = userService.getUserAvatarUrl("user-123");

        // Then
        assertEquals("http://example.com/avatar.jpg", result);
    }

    @Test
    void getUserAvatarUrl_Success_NoAvatar() {
        // Given
        testUser.setAvatar(null);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));

        // When
        String result = userService.getUserAvatarUrl("user-123");

        // Then
        assertEquals("/default-avatar.png", result);
    }

    @Test
    void getUserAvatarUrl_Failure_UserNotFound() {
        // Given
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserException.class, () -> userService.getUserAvatarUrl("nonexistent"));
    }

    // ===== 检查用户名/邮箱存在性测试 =====
    @Test
    void existsByUsername_Success_True() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When
        boolean result = userService.existsByUsername("testuser");

        // Then
        assertTrue(result);
    }

    @Test
    void existsByUsername_Success_False() {
        // Given
        when(userRepository.existsByUsername("nonexistent")).thenReturn(false);

        // When
        boolean result = userService.existsByUsername("nonexistent");

        // Then
        assertFalse(result);
    }

    @Test
    void existsByEmail_Success_True() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When
        boolean result = userService.existsByEmail("test@example.com");

        // Then
        assertTrue(result);
    }

    @Test
    void existsByEmail_Success_False() {
        // Given
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

        // When
        boolean result = userService.existsByEmail("nonexistent@example.com");

        // Then
        assertFalse(result);
    }

    // ===== 查找用户测试 =====
    @Test
    void findByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsername("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void findByUsername_Failure_NotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByUsername("nonexistent");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void findByEmail_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void findByEmail_Failure_NotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        // Then
        assertFalse(result.isPresent());
    }
}
