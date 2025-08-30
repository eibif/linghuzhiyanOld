package org.linghu.mybackend.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.linghu.mybackend.constants.SystemConstants;
import org.linghu.mybackend.domain.Experiment;
import org.linghu.mybackend.domain.ExperimentTask;
import org.linghu.mybackend.domain.User;
import org.linghu.mybackend.domain.UserRoleId;
import org.linghu.mybackend.domain.UserRoleRelation;
import org.linghu.mybackend.dto.LoginRequestDTO;
import org.linghu.mybackend.dto.LoginResponseDTO;
import org.linghu.mybackend.dto.ProfileUpdateDTO;
import org.linghu.mybackend.dto.UserDTO;
import org.linghu.mybackend.dto.UserRegistrationDTO;
import org.linghu.mybackend.exception.UserException;
import org.linghu.mybackend.repository.ExperimentAssignmentRepository;
import org.linghu.mybackend.repository.ExperimentRepository;
import org.linghu.mybackend.repository.ExperimentTaskRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.linghu.mybackend.repository.UserRoleRepository;
import org.linghu.mybackend.security.JwtTokenUtil;
import org.linghu.mybackend.service.LoginLogService;
import org.linghu.mybackend.service.UserService;
import org.linghu.mybackend.util.MinioUtil;
import org.linghu.mybackend.utils.JsonUtils;
import org.linghu.mybackend.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户服务实现类
 * 实现业务层面的用户操作，依赖于UserService处理核心领域操作
 */
@Service
public class UserServiceImpl implements UserService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final LoginLogService loginLogService;
    private final MinioUtil minioUtil;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ExperimentRepository experimentRepository;
    private final ExperimentAssignmentRepository experimentAssignmentRepository;
    private final ExperimentTaskRepository experimentTaskRepository;

    // 头像URL过期时间(秒)，默认1小时
    @Value("${minio.avatar.url.expiry:3600}")
    private int avatarUrlExpiry;

    @Value("${jwt.tokenHead}")
    private String tokenHead;

    @Value("${jwt.expiration}")
    private long expiration;

    public UserServiceImpl(
            AuthenticationManager authenticationManager,
            JwtTokenUtil jwtTokenUtil,
            PasswordEncoder passwordEncoder,
            LoginLogService loginLogService,
            MinioUtil minioUtil,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            ExperimentRepository experimentRepository,
            ExperimentAssignmentRepository experimentAssignmentRepository,
            ExperimentTaskRepository experimentTaskRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.passwordEncoder = passwordEncoder;
        this.loginLogService = loginLogService;
        this.minioUtil = minioUtil;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.experimentRepository = experimentRepository;
        this.experimentAssignmentRepository = experimentAssignmentRepository;
        this.experimentTaskRepository = experimentTaskRepository;
    }

    @Override
    @Transactional
    public UserDTO registerUser(UserRegistrationDTO registrationDTO) { // 检查用户名是否已存在
        if (existsByUsername(registrationDTO.getUsername())) {
            throw UserException.usernameAlreadyExists();
        }

        // 检查邮箱是否已存在
        if (existsByEmail(registrationDTO.getEmail())) {
            throw UserException.emailAlreadyExists();
        }

        // 创建用户实体
        User user = new User();
        user.setUsername(registrationDTO.getUsername());
        user.setEmail(registrationDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword())); // 对密码进行加密
        // 使用新的standardizeProfile方法处理profile
        user.setProfile(JsonUtils.standardizeProfile("{}"));

        // 保存用户
        User savedUser = createUser(user);

        // 默认分配学生角色
        assignRoleToUser(savedUser.getId(), SystemConstants.ROLE_STUDENT);
        //
        return convertToDTO(savedUser);
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {        // 获取当前请求信息
        String ipAddress = "unknown";
        String deviceType = "unknown";
        String loginInfo = "{}";

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = RequestUtils.getClientIpAddress(request);
            deviceType = RequestUtils.getDeviceType(request);
            loginInfo = RequestUtils.collectRequestInfo(request);
        }

        try {
            // 使用Spring Security进行身份验证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestDTO.getUsername(),
                            loginRequestDTO.getPassword()));
            // 获取认证成功的用户详情
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // 查找用户信息
            User user = findByUsername(userDetails.getUsername())
                    .orElseThrow(UserException::userNotFound);

            // 如果指定了角色，验证用户是否拥有该角色
            if (loginRequestDTO.getRole() != null && !loginRequestDTO.getRole().isEmpty()) {
                // 获取用户的所有角色ID
                Set<String> userRoles = getUserRoleIds(user.getId());

                // 检查用户是否拥有指定角色，考虑角色前缀
                String roleWithPrefix = loginRequestDTO.getRole();
                if (!roleWithPrefix.startsWith("ROLE_")) {
                    roleWithPrefix = "ROLE_" + roleWithPrefix;
                }

                if (!userRoles.contains(roleWithPrefix)) {
                    // 记录角色授权失败日志
                    loginLogService.logFailedLogin(
                            user.getId(),
                            ipAddress,
                            deviceType,
                            "角色未授权: " + loginRequestDTO.getRole(),
                            loginInfo
                    );
                    throw UserException.roleNotAuthorized();
                }
            }

            // 记录登录成功日志
            loginLogService.logSuccessfulLogin(
                    user.getId(),
                    ipAddress,
                    deviceType,
                    loginInfo
            );

            // 生成JWT token
            String token = jwtTokenUtil.generateToken(userDetails);

            // 构建响应
            return LoginResponseDTO.builder()
                    .user(convertToDTO(user))
                    .token(token)
                    .tokenType(tokenHead)
                    .expiresIn(expiration / 1000) // 转换为秒
                    .build();
        } catch (BadCredentialsException e) {
            // 记录密码错误日志（注意不能泄露敏感信息）
            loginLogService.logFailedLogin(
                    loginRequestDTO.getUsername(), // 这里只记录用户名，因为用户不存在
                    ipAddress,
                    deviceType,
                    "密码错误",
                    loginInfo
            );
            throw UserException.invalidCredentials();
        } catch (Exception e) {
            // 记录其他登录失败情况
            loginLogService.logFailedLogin(
                    loginRequestDTO.getUsername(),
                    ipAddress,
                    deviceType,
                    "登录失败: " + e.getMessage(),
                    loginInfo
            );
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteUser(String userId, String currentUsername) {
        checkUserNotDeleted(userId);
        // 验证当前用户
        User currentUser = findByUsername(currentUsername)
                .orElseThrow(UserException::userNotFound);

        // 验证要删除的用户
        User targetUser = findById(userId)
                .orElseThrow(UserException::userNotFound);

        // 获取当前用户角色
        Set<String> currentUserRoles = getUserRoleIds(currentUser.getId());

        // 只有管理员才能进行删除操作
        if (!currentUserRoles.contains(SystemConstants.ROLE_ADMIN)) {
            throw new RuntimeException("权限不足：只有管理员才能删除用户");
        }

        // 获取目标用户角色
        Set<String> targetUserRoles = getUserRoleIds(targetUser.getId());

        // 验证权限：管理员可以删除自己以及其他低于管理员权限的账户
        if (targetUserRoles.contains(SystemConstants.ROLE_ADMIN) && userId.equals(currentUser.getId())) {
            throw new RuntimeException("无法删除自己的账户");
        }

        // 执行软删除
        deleteUser(userId);
    }

    @Override
    public UserDTO getUserById(String id) {
        checkUserNotDeleted(id);

        User user = findById(id)
                .orElseThrow(UserException::userNotFound);
        return convertToDTO(user);
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        User user = findByUsername(username)
                .orElseThrow(UserException::userNotFound);
        return convertToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateUser(String id, UserDTO userDTO) {
        checkUserNotDeleted(id);
        // 检查用户是否存在
        User user = findById(id)
                .orElseThrow(UserException::userNotFound);

        // 如果用户名有更改，检查新用户名是否已存在
        if (!user.getUsername().equals(userDTO.getUsername()) &&
                existsByUsername(userDTO.getUsername())) {
            throw UserException.usernameAlreadyExists();
        }

        // 如果邮箱有更改，检查新邮箱是否已存在
        if (!user.getEmail().equals(userDTO.getEmail()) &&
                existsByEmail(userDTO.getEmail())) {
            throw UserException.emailAlreadyExists();
        }
        // 更新用户基本信息
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setAvatar(userDTO.getAvatar());
        // 使用standardizeProfile方法处理profile
        user.setProfile(JsonUtils.standardizeProfile(userDTO.getProfile()));

        // 更新用户
        User updatedUser = updateUser(user);

        // 处理角色更新
        if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
            // 获取当前用户角色
            Set<String> currentRoleIds = getUserRoleIds(id);

            // 需要添加的角色
            Set<String> rolesToAdd = new HashSet<>(userDTO.getRoles());
            rolesToAdd.removeAll(currentRoleIds);

            // 需要移除的角色
            Set<String> rolesToRemove = new HashSet<>(currentRoleIds);
            rolesToRemove.removeAll(userDTO.getRoles());

            // 添加新角色
            for (String roleId : rolesToAdd) {
                assignRoleToUser(id, roleId);
            }

            // 移除旧角色
            for (String roleId : rolesToRemove) {
                removeRoleFromUser(id, roleId);
            }
        }

        return convertToDTO(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        // 验证用户存在
        User user = findByUsername(username)
                .orElseThrow(UserException::userNotFound);
        checkUserNotDeleted(user.getId());
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw UserException.invalidOldPassword();
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        updateUser(user);
    }

    @Override
    public Page<UserDTO> listUsers(int pageNum, int pageSize) {
        // 页码从0开始计算
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        // 只查询未被软删除的用户
        Page<User> userPage = userRepository.findByIsDeletedFalse(pageable);

        // 转换为DTO
        return userPage.map(this::convertToDTO);
    }

    /**
     * 将User实体转换为UserDTO
     *
     * @param user 用户实体
     * @return 用户DTO
     */
    private UserDTO convertToDTO(User user) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());

        dto.setProfile(JsonUtils.parseObject(user.getProfile(), Object.class));
        dto.setRoles(getUserRoleIds(user.getId()));
        dto.setCreatedAt(dateFormat.format(user.getCreatedAt()));
        dto.setUpdatedAt(dateFormat.format(user.getUpdatedAt()));
        dto.setIsDeleted(user.getIsDeleted());

        return dto;
    }

    @Override
    @Transactional
    public UserDTO updateUserProfile(String username, ProfileUpdateDTO profileUpdateDTO) {
        // 检查用户是否存在
        User user = findByUsername(username)
                .orElseThrow(UserException::userNotFound);

        // 检查用户是否被软删除
        checkUserNotDeleted(user.getId());

        // 只更新允许的字段：头像和个人资料
        if (profileUpdateDTO.getAvatar() != null) {
            user.setAvatar(profileUpdateDTO.getAvatar());
        }

        if (profileUpdateDTO.getProfile() != null) {
            // 将Object类型的profile转换为JSON字符串，然后进行标准化处理
            String profileJson = JsonUtils.toJsonString(profileUpdateDTO.getProfile());
            user.setProfile(JsonUtils.standardizeProfile(profileJson));
        }

        // 更新用户
        User updatedUser = updateUser(user);

        // 转换为DTO返回
        return convertToDTO(updatedUser);
    }

    @Override
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User not authenticated");
        }
        return authentication.getName();
    }

    @Override
    public UserDTO getUserInfo(String userId) {
        return getUserByUsername(userId);
    }

    /**
     * 更新用户头像
     *
     * @param username 用户名
     * @param file     头像文件
     * @return 包含头像路径和URL的Map
     */
    @Override
    @Transactional
    public Map<String, String> updateUserAvatar(String username, MultipartFile file) {
        // 检查用户是否存在
        User user = findByUsername(username)
                .orElseThrow(UserException::userNotFound);

        // 检查用户是否被软删除
        checkUserNotDeleted(user.getId());

        try {
            // 删除旧头像
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                try {
                    minioUtil.deleteUserAvatar(user.getAvatar());
                } catch (Exception e) {
                    // 删除旧头像失败不影响新头像上传，记录日志即可
                    // TODO: 记录日志
                }
            }

            // 上传新头像
            String avatarPath = minioUtil.uploadUserAvatar(file, user.getId());
            user.setAvatar(avatarPath);
            updateUser(user);

            // 生成访问URL
            String avatarUrl = minioUtil.getAvatarPreviewUrl(avatarPath, avatarUrlExpiry);

            // 返回头像信息
            Map<String, String> result = new HashMap<>();
            result.put("avatarPath", avatarPath);
            result.put("avatarUrl", avatarUrl);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("上传头像失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取用户头像URL
     *
     * @param userId 用户ID
     * @return 头像访问URL
     */
    @Override
    public String getUserAvatarUrl(String userId) {
        User user = findById(userId)
                .orElseThrow(UserException::userNotFound);

        if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
            // 如果用户没有头像，返回默认头像URL
            return "/default-avatar.png";  // 或者其他默认头像路径
        }
        try {
            return minioUtil.getAvatarPreviewUrl(user.getAvatar(), avatarUrlExpiry);
        } catch (Exception e) {
            throw new RuntimeException("获取头像URL失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void setUserRole(String userId, String roleId, String currentUsername) {
        // 检查目标用户是否存在且未被删除
        checkUserNotDeleted(userId);


        User currentUser = findByUsername(currentUsername)
                .orElseThrow(UserException::userNotFound);
        Set<String> currentUserRoles = getUserRoleIds(currentUser.getId());

        if (!canAssignRole(currentUserRoles, roleId)) {
            // 权限不足，保持语义清晰
            throw new org.springframework.security.access.AccessDeniedException("权限不足：无法分配该角色");
        }

        Set<String> targetUserRoles = getUserRoleIds(userId);
        // 若用户已拥有该角色，则抛出业务异常（与权限不足403区分）
        if (targetUserRoles.contains(roleId)) {
            throw UserException.roleAlreadyAssigned();
        }

        assignRoleToUser(userId, roleId);
    }


    @Override
    @Transactional
    public User createUser(User user) {
        Date now = new Date();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setIsDeleted(false);

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        user.setUpdatedAt(new Date());

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setIsDeleted(true);
        user.setUpdatedAt(new Date());
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRoleId(String roleId) {
        List<UserRoleRelation> userRoles = userRoleRepository.findByIdRoleId(roleId);
        Set<String> userIds = userRoles.stream()
                .map(ur -> ur.getId().getUserId())
                .collect(Collectors.toSet());

        return userRepository.findAllById(userIds);
    }

    @Override
    @Transactional
    public User assignRoleToUser(String userId, String roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Check if user already has this role
        UserRoleId userRoleId = new UserRoleId(userId, roleId);
        if (userRoleRepository.existsById(userRoleId)) {
            return user;
        }

        // 创建新的用户角色关联，不再设置对象引用
        UserRoleRelation userRole = new UserRoleRelation(userId, roleId);

        // 保存关联
        userRoleRepository.save(userRole);
        return user;
    }

    @Override
    @Transactional
    public User removeRoleFromUser(String userId, String roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Delete user-role relationship if it exists
        UserRoleId userRoleId = new UserRoleId(userId, roleId);
        userRoleRepository.findById(userRoleId)
                .ifPresent(userRoleRepository::delete);

        // Clear and refresh the user to avoid ConcurrentModificationException
        user = userRepository.findById(userId).orElse(user);

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getUserRoleIds(String userId) {
        List<UserRoleRelation> userRoles = userRoleRepository.findByIdUserId(userId);
        return userRoles.stream()
                .map(ur -> ur.getId().getRoleId())
                .collect(Collectors.toSet());
    }

    //检查当前用户是否可以分配指定角色
    private boolean canAssignRole(Set<String> currentUserRoles, String targetRoleId) {
        if (currentUserRoles.contains(SystemConstants.ROLE_ADMIN)) {
            return true;
        }

        if (currentUserRoles.contains(SystemConstants.ROLE_TEACHER)) {
            return SystemConstants.ROLE_TEACHER.equals(targetRoleId) || SystemConstants.ROLE_ASSISTANT.equals(targetRoleId);
        }

        if (currentUserRoles.contains(SystemConstants.ROLE_ASSISTANT)) {
            return SystemConstants.ROLE_ASSISTANT.equals(targetRoleId);
        }

        return false;
    }

    public void checkUserNotDeleted(String userId) {
        User user = findById(userId)
                .orElseThrow(UserException::userNotFound);

        if (user.getIsDeleted()) {
            throw UserException.userDeleted();
        }
    }

    @Override
    public List<String> getVisibleExperimentIdsForUser(String userId) {
        Set<String> roles = getUserRoleIds(userId);
        if (roles.contains(SystemConstants.ROLE_ADMIN) || roles.contains(SystemConstants.ROLE_TEACHER) || roles.contains(SystemConstants.ROLE_ASSISTANT)) {
            // 管理员/教师/助教可见全部实验
            return experimentRepository.findAll().stream().map(Experiment::getId).collect(java.util.stream.Collectors.toList());
        } else {
            // 学生：查分配到的任务，取出实验ID去重
            java.util.List<String> taskIds = experimentAssignmentRepository.findTaskIdsByUserId(userId);
            if (taskIds.isEmpty()) return java.util.Collections.emptyList();
            java.util.List<ExperimentTask> tasks = experimentTaskRepository.findAllById(taskIds);
            return tasks.stream().map(ExperimentTask::getExperimentId).distinct().collect(java.util.stream.Collectors.toList());
        }
    }
}
