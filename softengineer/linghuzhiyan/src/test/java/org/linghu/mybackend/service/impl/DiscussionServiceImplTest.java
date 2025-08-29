package org.linghu.mybackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.dto.DiscussionRequestDTO;
import org.linghu.mybackend.dto.DiscussionResponseDTO;
import org.linghu.mybackend.dto.PriorityRequestDTO;
import org.linghu.mybackend.dto.ReviewRequestDTO;
import org.linghu.mybackend.dto.UserDTO;
import org.linghu.mybackend.domain.Discussion;
import org.linghu.mybackend.exception.ResourceNotFoundException;
import org.linghu.mybackend.exception.BusinessException;
import org.linghu.mybackend.repository.DiscussionRepository;
import org.linghu.mybackend.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * 讨论服务实现类测试
 * 从微服务架构迁移到单体架构，保持原有测试点覆盖
 * 包含讨论创建、查询、更新、删除、审核、点赞、优先级管理、统计等功能的正反测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("讨论服务实现类测试")
class DiscussionServiceImplTest {

    @Mock
    private DiscussionRepository discussionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private DiscussionServiceImpl discussionService;

    private UserDTO testUser;
    private UserDTO otherUser;
    private Discussion testDiscussion;
    private Discussion testDiscussion2;
    private DiscussionRequestDTO validCreateRequest;
    private DiscussionRequestDTO validUpdateRequest;

    @BeforeEach
    void setUp() {
        // 创建测试用户DTO
        testUser = new UserDTO();
        testUser.setId("user1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        otherUser = new UserDTO();
        otherUser.setId("user2");
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");

        // 创建测试讨论
        testDiscussion = new Discussion();
        testDiscussion.setId("discussion1");
        testDiscussion.setTitle("Test Discussion");
        testDiscussion.setContent("Test content");
        testDiscussion.setUserId("user1");
        testDiscussion.setUsername("testuser");
        testDiscussion.setTags(Arrays.asList("programming", "java"));
        testDiscussion.setExperimentId("exp1");
        testDiscussion.setStatus("PENDING");
        testDiscussion.setPriority(0);
        testDiscussion.setViewCount(0L);
        testDiscussion.setCommentCount(0L);
        testDiscussion.setLikeCount(0L);
        testDiscussion.setLikedBy(new ArrayList<>());
        testDiscussion.setDeleted(false);
        testDiscussion.setCreateTime(LocalDateTime.now());
        testDiscussion.setUpdateTime(LocalDateTime.now());

        testDiscussion2 = new Discussion();
        testDiscussion2.setId("discussion2");
        testDiscussion2.setTitle("Another Discussion");
        testDiscussion2.setContent("Another content");
        testDiscussion2.setUserId("user2");
        testDiscussion2.setUsername("otheruser");
        testDiscussion2.setTags(Arrays.asList("algorithms", "python"));
        testDiscussion2.setExperimentId("exp2");
        testDiscussion2.setStatus("APPROVED");
        testDiscussion2.setPriority(1);
        testDiscussion2.setViewCount(10L);
        testDiscussion2.setCommentCount(5L);
        testDiscussion2.setLikeCount(3L);
        testDiscussion2.setLikedBy(Arrays.asList("user1", "user3", "user4"));
        testDiscussion2.setDeleted(false);
        testDiscussion2.setCreateTime(LocalDateTime.now().minusDays(1));
        testDiscussion2.setUpdateTime(LocalDateTime.now());

        // 创建有效的讨论创建请求
        validCreateRequest = new DiscussionRequestDTO();
        validCreateRequest.setTitle("New Discussion");
        validCreateRequest.setContent("New discussion content");
        validCreateRequest.setTags(Arrays.asList("test", "example"));
        validCreateRequest.setExperimentId("exp1");

        // 创建有效的更新请求
        validUpdateRequest = new DiscussionRequestDTO();
        validUpdateRequest.setTitle("Updated Discussion");
        validUpdateRequest.setContent("Updated content");
        validUpdateRequest.setTags(Arrays.asList("updated", "test"));
    }

    @Nested
    @DisplayName("创建讨论测试")
    class CreateDiscussionTests {

        @Test
        @DisplayName("成功创建讨论")
        void shouldCreateDiscussionSuccessfully() {
            // Given
            given(userService.getUserInfo("user1")).willReturn(testUser);
            given(discussionRepository.save(any(Discussion.class))).willReturn(testDiscussion);

            // When
            DiscussionResponseDTO result = discussionService.createDiscussion(validCreateRequest, "user1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("discussion1");
            assertThat(result.getTitle()).isEqualTo("Test Discussion");
            assertThat(result.getContent()).isEqualTo("Test content");
            assertThat(result.getUserId()).isEqualTo("user1");
            assertThat(result.getStatus()).isEqualTo("PENDING");

            verify(userService).getUserInfo("user1");
            verify(discussionRepository).save(any(Discussion.class));
        }

        @Test
        @DisplayName("标题为空时抛出业务异常")
        void shouldThrowBusinessExceptionWhenTitleIsEmpty() {
            // Given
            DiscussionRequestDTO invalidRequest = new DiscussionRequestDTO();
            invalidRequest.setTitle("");
            invalidRequest.setContent("Valid content");

            // When & Then
            assertThatThrownBy(() -> discussionService.createDiscussion(invalidRequest, "user1"))
                    .isInstanceOf(BusinessException.class);

            verify(discussionRepository, never()).save(any(Discussion.class));
        }

        @Test
        @DisplayName("内容为空时抛出业务异常")
        void shouldThrowBusinessExceptionWhenContentIsEmpty() {
            // Given
            DiscussionRequestDTO invalidRequest = new DiscussionRequestDTO();
            invalidRequest.setTitle("Valid title");
            invalidRequest.setContent("");

            // When & Then
            assertThatThrownBy(() -> discussionService.createDiscussion(invalidRequest, "user1"))
                    .isInstanceOf(BusinessException.class);

            verify(discussionRepository, never()).save(any(Discussion.class));
        }

        @Test
        @DisplayName("标题过长时抛出业务异常")
        void shouldThrowBusinessExceptionWhenTitleTooLong() {
            // Given
            DiscussionRequestDTO invalidRequest = new DiscussionRequestDTO();
            invalidRequest.setTitle("a".repeat(201)); // 超过200字符限制
            invalidRequest.setContent("Valid content");

            // When & Then
            assertThatThrownBy(() -> discussionService.createDiscussion(invalidRequest, "user1"))
                    .isInstanceOf(BusinessException.class);

            verify(discussionRepository, never()).save(any(Discussion.class));
        }

        @Test
        @DisplayName("内容过长时抛出业务异常")
        void shouldThrowBusinessExceptionWhenContentTooLong() {
            // Given
            DiscussionRequestDTO invalidRequest = new DiscussionRequestDTO();
            invalidRequest.setTitle("Valid title");
            invalidRequest.setContent("a".repeat(10001)); // 超过10000字符限制

            // When & Then
            assertThatThrownBy(() -> discussionService.createDiscussion(invalidRequest, "user1"))
                    .isInstanceOf(BusinessException.class);

            verify(discussionRepository, never()).save(any(Discussion.class));
        }
    }

    @Nested
    @DisplayName("获取讨论测试")
    class GetDiscussionTests {

        @Test
        @DisplayName("成功获取讨论列表")
        void shouldGetDiscussionsSuccessfully() {
            // Given
            List<Discussion> discussions = Arrays.asList(testDiscussion, testDiscussion2);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Discussion> discussionPage = new PageImpl<>(discussions, pageable, 2);

            given(discussionRepository.findAllNonDeleted(any(Pageable.class))).willReturn(discussionPage);

            // When
            Page<DiscussionResponseDTO> result = discussionService.getDiscussions(
                    null, null, null, null, null, 
                    "createTime", "desc", 0, 10, "user1", "USER");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getId()).isEqualTo("discussion1");
            assertThat(result.getContent().get(1).getId()).isEqualTo("discussion2");

            verify(discussionRepository).findAllNonDeleted(any(Pageable.class));
        }

        @Test
        @DisplayName("成功根据ID获取讨论详情")
        void shouldGetDiscussionByIdSuccessfully() {
            // Given
            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));

            // When
            DiscussionResponseDTO result = discussionService.getDiscussionById("discussion1", "user1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("discussion1");
            assertThat(result.getTitle()).isEqualTo("Test Discussion");
            assertThat(result.getContent()).isEqualTo("Test content");

            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
        }

        @Test
        @DisplayName("讨论不存在时抛出资源未找到异常")
        void shouldThrowResourceNotFoundExceptionWhenDiscussionNotExists() {
            // Given
            given(discussionRepository.findByIdAndNotDeleted("nonexistent")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> discussionService.getDiscussionById("nonexistent", "user1"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(discussionRepository).findByIdAndNotDeleted("nonexistent");
        }

        @Test
        @DisplayName("成功增加浏览计数")
        void shouldIncrementViewCountSuccessfully() {
            // Given
            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));
            given(discussionRepository.save(any(Discussion.class))).willReturn(testDiscussion);

            // When
            discussionService.incrementViewCount("discussion1");

            // Then
            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository).save(testDiscussion);
            assertThat(testDiscussion.getViewCount()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("更新讨论测试")
    class UpdateDiscussionTests {

        @Test
        @DisplayName("作者成功更新讨论")
        void shouldUpdateDiscussionByAuthorSuccessfully() {
            // Given
            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));
            given(discussionRepository.save(any(Discussion.class))).willReturn(testDiscussion);

            // When
            DiscussionResponseDTO result = discussionService.updateDiscussion("discussion1", validUpdateRequest, "user1");

            // Then
            assertThat(result).isNotNull();
            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository).save(testDiscussion);
        }

        @Test
        @DisplayName("非作者更新讨论时抛出业务异常")
        void shouldThrowBusinessExceptionWhenNonAuthorUpdatesDiscussion() {
            // Given
            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));

            // When & Then
            assertThatThrownBy(() -> discussionService.updateDiscussion("discussion1", validUpdateRequest, "user2"))
                    .isInstanceOf(BusinessException.class);

            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository, never()).save(any(Discussion.class));
        }

        @Test
        @DisplayName("讨论不存在时抛出资源未找到异常")
        void shouldThrowResourceNotFoundExceptionWhenUpdatingNonexistentDiscussion() {
            // Given
            given(discussionRepository.findByIdAndNotDeleted("nonexistent")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> discussionService.updateDiscussion("nonexistent", validUpdateRequest, "user1"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(discussionRepository).findByIdAndNotDeleted("nonexistent");
            verify(discussionRepository, never()).save(any(Discussion.class));
        }

        @Test
        @DisplayName("已批准的讨论不能更新时抛出业务异常")
        void shouldThrowBusinessExceptionWhenUpdatingApprovedDiscussion() {
            // Given
            testDiscussion.setStatus("APPROVED");
            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));

            // When & Then
            assertThatThrownBy(() -> discussionService.updateDiscussion("discussion1", validUpdateRequest, "user1"))
                    .isInstanceOf(BusinessException.class);

            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository, never()).save(any(Discussion.class));
        }
    }

    @Nested
    @DisplayName("删除讨论测试")
    class DeleteDiscussionTests {

        @Test
        @DisplayName("作者成功删除讨论")
        void shouldDeleteDiscussionByAuthorSuccessfully() {
            // Given
            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));
            given(discussionRepository.save(any(Discussion.class))).willReturn(testDiscussion);

            // When
            boolean result = discussionService.deleteDiscussion("discussion1", "user1");

            // Then
            assertThat(result).isTrue();
            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository).save(testDiscussion);
            assertThat(testDiscussion.getDeleted()).isTrue();
        }

        @Test
        @DisplayName("非作者删除讨论时抛出业务异常")
        void shouldThrowBusinessExceptionWhenNonAuthorDeletesDiscussion() {
            // Given
            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));

            // When & Then
            assertThatThrownBy(() -> discussionService.deleteDiscussion("discussion1", "user2"))
                    .isInstanceOf(BusinessException.class);

            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository, never()).save(any(Discussion.class));
        }

        @Test
        @DisplayName("讨论不存在时抛出资源未找到异常")
        void shouldThrowResourceNotFoundExceptionWhenDeletingNonexistentDiscussion() {
            // Given
            given(discussionRepository.findByIdAndNotDeleted("nonexistent")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> discussionService.deleteDiscussion("nonexistent", "user1"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(discussionRepository).findByIdAndNotDeleted("nonexistent");
            verify(discussionRepository, never()).save(any(Discussion.class));
        }
    }

    @Nested
    @DisplayName("审核讨论测试")
    class ReviewDiscussionTests {

        @Test
        @DisplayName("成功批准讨论")
        void shouldApproveDiscussionSuccessfully() {
            // Given
            ReviewRequestDTO reviewRequest = new ReviewRequestDTO();
            reviewRequest.setStatus("APPROVED");

            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));
            given(discussionRepository.save(any(Discussion.class))).willReturn(testDiscussion);

            // When
            DiscussionResponseDTO result = discussionService.reviewDiscussion("discussion1", reviewRequest, "reviewer1");

            // Then
            assertThat(result).isNotNull();
            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository).save(testDiscussion);
            assertThat(testDiscussion.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("成功拒绝讨论")
        void shouldRejectDiscussionSuccessfully() {
            // Given
            ReviewRequestDTO reviewRequest = new ReviewRequestDTO();
            reviewRequest.setStatus("REJECTED");
            reviewRequest.setRejectionReason("Content inappropriate");

            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));
            given(discussionRepository.save(any(Discussion.class))).willReturn(testDiscussion);

            // When
            DiscussionResponseDTO result = discussionService.reviewDiscussion("discussion1", reviewRequest, "reviewer1");

            // Then
            assertThat(result).isNotNull();
            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository).save(testDiscussion);
            assertThat(testDiscussion.getStatus()).isEqualTo("REJECTED");
            assertThat(testDiscussion.getRejectionReason()).isEqualTo("Content inappropriate");
        }

        @Test
        @DisplayName("讨论不存在时抛出资源未找到异常")
        void shouldThrowResourceNotFoundExceptionWhenReviewingNonexistentDiscussion() {
            // Given
            ReviewRequestDTO reviewRequest = new ReviewRequestDTO();
            reviewRequest.setStatus("APPROVED");

            given(discussionRepository.findByIdAndNotDeleted("nonexistent")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> discussionService.reviewDiscussion("nonexistent", reviewRequest, "reviewer1"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(discussionRepository).findByIdAndNotDeleted("nonexistent");
            verify(discussionRepository, never()).save(any(Discussion.class));
        }

        @Test
        @DisplayName("无效审核状态时抛出业务异常")
        void shouldThrowBusinessExceptionWhenInvalidReviewStatus() {
            // Given
            ReviewRequestDTO reviewRequest = new ReviewRequestDTO();
            reviewRequest.setStatus("INVALID_STATUS");

            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));

            // When & Then
            assertThatThrownBy(() -> discussionService.reviewDiscussion("discussion1", reviewRequest, "reviewer1"))
                    .isInstanceOf(BusinessException.class);

            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository, never()).save(any(Discussion.class));
        }
    }

    @Nested
    @DisplayName("优先级管理测试")
    class PriorityManagementTests {

        @Test
        @DisplayName("成功更新讨论优先级")
        void shouldUpdatePrioritySuccessfully() {
            // Given
            PriorityRequestDTO priorityRequest = new PriorityRequestDTO();
            priorityRequest.setPriority(5);

            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));
            given(discussionRepository.save(any(Discussion.class))).willReturn(testDiscussion);

            // When
            DiscussionResponseDTO result = discussionService.updatePriority("discussion1", priorityRequest, "admin1");

            // Then
            assertThat(result).isNotNull();
            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository).save(testDiscussion);
            assertThat(testDiscussion.getPriority()).isEqualTo(5);
        }

        @Test
        @DisplayName("讨论不存在时抛出资源未找到异常")
        void shouldThrowResourceNotFoundExceptionWhenUpdatingPriorityOfNonexistentDiscussion() {
            // Given
            PriorityRequestDTO priorityRequest = new PriorityRequestDTO();
            priorityRequest.setPriority(3);

            given(discussionRepository.findByIdAndNotDeleted("nonexistent")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> discussionService.updatePriority("nonexistent", priorityRequest, "admin1"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(discussionRepository).findByIdAndNotDeleted("nonexistent");
            verify(discussionRepository, never()).save(any(Discussion.class));
        }
    }

    @Nested
    @DisplayName("点赞功能测试")
    class ToggleLikeTests {

        @Test
        @DisplayName("成功点赞讨论")
        void shouldToggleLikeSuccessfully() {
            // Given
            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));
            given(discussionRepository.save(any(Discussion.class))).willReturn(testDiscussion);

            // When
            DiscussionResponseDTO result = discussionService.toggleLike("discussion1", "user2");

            // Then
            assertThat(result).isNotNull();
            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository).save(testDiscussion);
            assertThat(testDiscussion.getLikedBy()).contains("user2");
            assertThat(testDiscussion.getLikeCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("成功取消点赞讨论")
        void shouldToggleUnlikeSuccessfully() {
            // Given
            testDiscussion.getLikedBy().add("user2");
            testDiscussion.setLikeCount(1L);
            given(discussionRepository.findByIdAndNotDeleted("discussion1")).willReturn(Optional.of(testDiscussion));
            given(discussionRepository.save(any(Discussion.class))).willReturn(testDiscussion);

            // When
            DiscussionResponseDTO result = discussionService.toggleLike("discussion1", "user2");

            // Then
            assertThat(result).isNotNull();
            verify(discussionRepository).findByIdAndNotDeleted("discussion1");
            verify(discussionRepository).save(testDiscussion);
            assertThat(testDiscussion.getLikedBy()).doesNotContain("user2");
            assertThat(testDiscussion.getLikeCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("讨论不存在时抛出资源未找到异常")
        void shouldThrowResourceNotFoundExceptionWhenDiscussionNotExistsForToggleLike() {
            // Given
            given(discussionRepository.findByIdAndNotDeleted("nonexistent")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> discussionService.toggleLike("nonexistent", "user1"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(discussionRepository).findByIdAndNotDeleted("nonexistent");
            verify(discussionRepository, never()).save(any(Discussion.class));
        }
    }

    @Nested
    @DisplayName("边界条件和异常情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("批量获取实验相关讨论")
        void shouldGetDiscussionsByExperimentIdsSuccessfully() {
            // Given
            List<String> experimentIds = Arrays.asList("exp1", "exp2");
            List<Discussion> discussionsExp1 = Arrays.asList(testDiscussion);
            List<Discussion> discussionsExp2 = Arrays.asList(testDiscussion2);

            given(discussionRepository.findByStatusAndExperimentId("APPROVED", "exp1", PageRequest.of(0, 100)).getContent())
                    .willReturn(discussionsExp1);
            given(discussionRepository.findByStatusAndExperimentId("APPROVED", "exp2", PageRequest.of(0, 100)).getContent())
                    .willReturn(discussionsExp2);

            // When
            List<DiscussionResponseDTO> result = discussionService.getDiscussionsByExperimentIds(experimentIds);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo("discussion1");
            assertThat(result.get(1).getId()).isEqualTo("discussion2");

            // 由于方法实现可能使用不同的查询逻辑，这里验证至少调用了某些repository方法
            verify(discussionRepository, atLeastOnce()).findByStatusAndExperimentId(eq("APPROVED"), anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("空实验ID列表返回空结果")
        void shouldReturnEmptyListForEmptyExperimentIds() {
            // Given
            List<String> emptyList = Collections.emptyList();

            // When
            List<DiscussionResponseDTO> result = discussionService.getDiscussionsByExperimentIds(emptyList);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(discussionRepository, never()).findByStatusAndExperimentId(anyString(), anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("处理分页参数边界情况")
        void shouldHandlePaginationEdgeCases() {
            // Given
            List<Discussion> discussions = Arrays.asList(testDiscussion);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Discussion> discussionPage = new PageImpl<>(discussions, pageable, 1);

            given(discussionRepository.findAllNonDeleted(any(Pageable.class))).willReturn(discussionPage);

            // When - 使用负数页码和大小
            Page<DiscussionResponseDTO> result = discussionService.getDiscussions(
                    null, null, null, null, null, 
                    "createTime", "asc", -1, 0, "user1", "USER");

            // Then
            assertThat(result).isNotNull();
            verify(discussionRepository).findAllNonDeleted(any(Pageable.class));
        }

        @Test
        @DisplayName("处理无效排序参数")
        void shouldHandleInvalidSortParameters() {
            // Given
            List<Discussion> discussions = Arrays.asList(testDiscussion);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Discussion> discussionPage = new PageImpl<>(discussions, pageable, 1);

            given(discussionRepository.findAllNonDeleted(any(Pageable.class))).willReturn(discussionPage);

            // When - 使用无效排序字段
            Page<DiscussionResponseDTO> result = discussionService.getDiscussions(
                    null, null, null, null, null, 
                    "invalidField", "invalidOrder", 0, 10, "user1", "USER");

            // Then
            assertThat(result).isNotNull();
            verify(discussionRepository).findAllNonDeleted(any(Pageable.class));
        }
    }
}
