package org.linghu.mybackend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.linghu.mybackend.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserRepository 测试类 
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = {"/schema.sql", "/data.sql"})
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;


    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setUsername("newtestuser");
        testUser.setEmail("newtest@example.com");
        testUser.setPassword("encoded-password");
        testUser.setProfile("{}");
        testUser.setIsDeleted(false);
        testUser.setCreatedAt(new Date());
        testUser.setUpdatedAt(new Date());
    }

    @Test
    void printAllUsers() {
        userRepository.findAll().forEach(u ->
                System.out.println(u.getUsername() + " | " + u.getEmail() + " | " + u.getIsDeleted())
        );
    }
    // ===== findByUsername 测试 =====
    @Test
    void findByUsername_Success_ExistingUser() {
        // When
        Optional<User> result = userRepository.findByUsername("testuser1");
        result.ifPresent(user -> System.out.println(
                "id=" + user.getId() +
                        ", username=" + user.getUsername() +
                        ", email=" + user.getEmail() +
                        ", isDeleted=" + user.getIsDeleted()
        ));
        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser1", result.get().getUsername());
        assertEquals("test1@example.com", result.get().getEmail());
    }

    @Test
    void findByUsername_Failure_NonExistingUser() {
        // When
        Optional<User> result = userRepository.findByUsername("nonexistentuser");
        
        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void findByUsername_Success_DeletedUser() {
        // Given - 已删除的用户也能被查询到
        
        // When
        Optional<User> result = userRepository.findByUsername("deleteduser");
        
        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get().getIsDeleted());
    }

    // ===== findByEmail 测试 =====
    @Test
    void findByEmail_Success_ExistingEmail() {
        // When
        Optional<User> result = userRepository.findByEmail("test1@example.com");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser1", result.get().getUsername());
        assertEquals("test1@example.com", result.get().getEmail());
    }

    @Test
    void findByEmail_Failure_NonExistingEmail() {
        // When
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");
        
        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void findByEmail_Success_DeletedUserEmail() {
        // When
        Optional<User> result = userRepository.findByEmail("deleted@example.com");
        
        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get().getIsDeleted());
    }

    // ===== existsByUsername 测试 =====
    @Test
    void existsByUsername_Success_ExistingUser() {
        // When
        boolean exists = userRepository.existsByUsername("testuser1");
        
        // Then
        assertTrue(exists);
    }

    @Test
    void existsByUsername_Failure_NonExistingUser() {
        // When
        boolean exists = userRepository.existsByUsername("nonexistentuser");
        
        // Then
        assertFalse(exists);
    }

    @Test
    void existsByUsername_Success_DeletedUser() {
        // Given - 已删除的用户也会被认为存在
        
        // When
        boolean exists = userRepository.existsByUsername("deleteduser");
        
        // Then
        assertTrue(exists);
    }

    // ===== existsByEmail 测试 =====
    @Test
    void existsByEmail_Success_ExistingEmail() {
        // When
        boolean exists = userRepository.existsByEmail("test1@example.com");
        
        // Then
        assertTrue(exists);
    }

    @Test
    void existsByEmail_Failure_NonExistingEmail() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");
        
        // Then
        assertFalse(exists);
    }

    @Test
    void existsByEmail_Success_DeletedUserEmail() {
        // When
        Boolean exists = userRepository.existsByEmail("deleted@example.com");
        
        // Then
        assertTrue(exists);
    }

    // ===== findByIsDeletedFalse 测试 =====
    @Test
    void findByIsDeletedFalse_Success_ReturnsOnlyActiveUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<User> result = userRepository.findByIsDeletedFalse(pageable);
        
        // Then
        assertNotNull(result);
        assertTrue(result.getTotalElements() >= 3); // 至少有3个未删除用户
        
        // 验证所有返回的用户都未被删除
        result.getContent().forEach(user -> 
            assertFalse(user.getIsDeleted(), "用户 " + user.getUsername() + " 应该未被删除")
        );
    }

    @Test
    void findByIsDeletedFalse_Success_Pagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2); // 每页2个
        
        // When
        Page<User> result = userRepository.findByIsDeletedFalse(pageable);
        
        // Then
        assertNotNull(result);
        assertTrue(result.getContent().size() <= 2);
        assertEquals(0, result.getNumber()); // 当前页码
        assertEquals(2, result.getSize()); // 页面大小
    }

    @Test
    void findByIsDeletedFalse_Success_EmptyResult() {
        // Given - 先删除所有用户
        userRepository.findAll().forEach(user -> {
            user.setIsDeleted(true);
            userRepository.save(user);
        });
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<User> result = userRepository.findByIsDeletedFalse(pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    // ===== JPA 基本操作测试 =====
    @Test
    void save_Success_NewUser() {
        // When
        User savedUser = userRepository.save(testUser);
        
        // Then
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals("newtestuser", savedUser.getUsername());
        assertEquals("newtest@example.com", savedUser.getEmail());
        assertFalse(savedUser.getIsDeleted());
    }

    @Test
    void findById_Success_ExistingUser() {
        // Given
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();
        
        // When
        Optional<User> result = userRepository.findById(savedUser.getId());
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(savedUser.getId(), result.get().getId());
        assertEquals("newtestuser", result.get().getUsername());
    }

    @Test
    void findById_Failure_NonExistingUser() {
        // When
        Optional<User> result = userRepository.findById("non-existent-id");
        
        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void delete_Success_UserRemoved() {
        // Given
        User savedUser = userRepository.save(testUser);
        String userId = savedUser.getId();
        entityManager.flush();
        
        // When
        userRepository.delete(savedUser);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        Optional<User> result = userRepository.findById(userId);
        assertFalse(result.isPresent());
    }

    @Test
    void update_Success_UserModified() {
        // Given
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();
        
        // When
        Optional<User> userOpt = userRepository.findById(savedUser.getId());
        assertTrue(userOpt.isPresent());
        
        User user = userOpt.get();
        user.setEmail("updated@example.com");
        user.setProfile("{\"nickname\":\"Updated User\"}");
        User updatedUser = userRepository.save(user);
        entityManager.flush();
        
        // Then
        assertEquals("updated@example.com", updatedUser.getEmail());
        assertEquals("{\"nickname\":\"Updated User\"}", updatedUser.getProfile());
    }

    // ===== 约束测试 =====
    @Test
    void save_Failure_DuplicateUsername() {
        // Given
        userRepository.save(testUser);
        entityManager.flush();
        
        User duplicateUser = new User();
        duplicateUser.setUsername("newtestuser"); // 重复用户名
        duplicateUser.setEmail("different@example.com");
        duplicateUser.setPassword("password");
        duplicateUser.setProfile("{}");
        duplicateUser.setIsDeleted(false);
        
        // When & Then
        assertThrows(Exception.class, () -> {
            userRepository.save(duplicateUser);
            entityManager.flush();
        });
    }

    @Test
    void save_Failure_DuplicateEmail() {
        // Given
        userRepository.save(testUser);
        entityManager.flush();
        
        User duplicateUser = new User();
        duplicateUser.setUsername("differentuser");
        duplicateUser.setEmail("newtest@example.com"); // 重复邮箱
        duplicateUser.setPassword("password");
        duplicateUser.setProfile("{}");
        duplicateUser.setIsDeleted(false);
        
        // When & Then
        assertThrows(Exception.class, () -> {
            userRepository.save(duplicateUser);
            entityManager.flush();
        });
    }

    // ===== 性能测试 =====
    @Test
    void findByUsername_Performance_MultipleQueries() {
        // Given
        String[] usernames = {"testuser1", "testuser2", "adminuser", "nonexistent"};
        
        // When & Then
        for (String username : usernames) {
            long startTime = System.currentTimeMillis();
            Optional<User> result = userRepository.findByUsername(username);
            long endTime = System.currentTimeMillis();
            
            // 验证查询时间合理（应该很快）
            assertTrue(endTime - startTime < 100, "查询时间应该小于100ms");
        }
    }

    @Test
    void findByIsDeletedFalse_Performance_LargePage() {
        // Given
        Pageable largePage = PageRequest.of(0, 1000);
        
        // When
        long startTime = System.currentTimeMillis();
        Page<User> result = userRepository.findByIsDeletedFalse(largePage);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertNotNull(result);
        assertTrue(endTime - startTime < 1000, "大页面查询时间应该小于1秒");
    }
}
