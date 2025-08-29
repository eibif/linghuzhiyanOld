package org.linghu.mybackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.domain.Comment;
import org.linghu.mybackend.domain.Discussion;
import org.linghu.mybackend.dto.CommentRequestDTO;
import org.linghu.mybackend.dto.CommentResponseDTO;
import org.linghu.mybackend.dto.ReportCommentDTO;
import org.linghu.mybackend.dto.UserDTO;
import org.linghu.mybackend.exception.ResourceNotFoundException;
import org.linghu.mybackend.exception.UnauthorizedException;
import org.linghu.mybackend.repository.CommentRepository;
import org.linghu.mybackend.repository.DiscussionRepository;
import org.linghu.mybackend.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl 完整测试")
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private DiscussionRepository discussionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private UserDTO testUser;
    private Discussion testDiscussion;
    private Comment testComment;
    private CommentRequestDTO commentRequestDTO;

    @BeforeEach
    void setUp() {
        testUser = UserDTO.builder()
                .id("user1")
                .username("testuser")
                .avatar("avatar.jpg")
                .build();

        testDiscussion = Discussion.builder()
                .id("discussion1")
                .title("测试讨论")
                .userId("user1")
                .commentCount(0L)
                .deleted(false)
                .build();

        testComment = Comment.builder()
                .id("comment1")
                .discussionId("discussion1")
                .content("测试评论")
                .userId("user1")
                .username("testuser")
                .userAvatar("avatar.jpg")
                .depth(0)
                .likeCount(0)
                .likedBy(new ArrayList<>())
                .path("comment1")
                .deleted(false)
                .status("VISIBLE")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        commentRequestDTO = CommentRequestDTO.builder()
                .content("测试评论内容")
                .build();
    }

    @Nested
    @DisplayName("创建评论测试")
    class CreateCommentTests {

        @Test
        @DisplayName("成功创建根评论")
        void shouldCreateRootCommentSuccessfully() {
            // Given
            when(discussionRepository.findByIdAndNotDeleted("discussion1"))
                    .thenReturn(Optional.of(testDiscussion));
            when(userService.getUserInfo("user1")).thenReturn(testUser);
            
            // 模拟两次save调用 - 第一次保存返回没有path的comment，第二次保存返回有path的comment
            Comment firstSave = Comment.builder()
                    .id("comment1")
                    .discussionId("discussion1")
                    .content("测试评论内容")
                    .userId("user1")
                    .username("testuser")
                    .userAvatar("avatar.jpg")
                    .depth(0)
                    .likeCount(0)
                    .likedBy(new ArrayList<>())
                    .deleted(false)
                    .status("VISIBLE")
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
                    
            Comment secondSave = Comment.builder()
                    .id("comment1")
                    .discussionId("discussion1")
                    .content("测试评论内容")
                    .userId("user1")
                    .username("testuser")
                    .userAvatar("avatar.jpg")
                    .depth(0)
                    .likeCount(0)
                    .likedBy(new ArrayList<>())
                    .path("comment1")
                    .deleted(false)
                    .status("VISIBLE")
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
                    
            when(commentRepository.save(any(Comment.class)))
                    .thenReturn(firstSave)
                    .thenReturn(secondSave);

            // When
            CommentResponseDTO result = commentService.createComment("discussion1", commentRequestDTO, "user1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("测试评论内容");
            assertThat(result.getUserId()).isEqualTo("user1");
            verify(commentRepository, times(2)).save(any(Comment.class)); // 两次保存
            verify(discussionRepository).save(any(Discussion.class));
        }

        @Test
        @DisplayName("成功创建回复评论")
        void shouldCreateReplyCommentSuccessfully() {
            // Given
            Comment parentComment = Comment.builder()
                    .id("parent1")
                    .discussionId("discussion1")
                    .content("父评论")
                    .userId("user2")
                    .depth(0)
                    .path("parent1")
                    .deleted(false)
                    .build();

            CommentRequestDTO replyRequestDTO = CommentRequestDTO.builder()
                    .content("回复内容")
                    .parentId("parent1")
                    .replyToUserId("user2")
                    .build();

            UserDTO replyToUser = UserDTO.builder()
                    .id("user2")
                    .username("parentuser")
                    .build();

            when(discussionRepository.findByIdAndNotDeleted("discussion1"))
                    .thenReturn(Optional.of(testDiscussion));
            when(userService.getUserInfo("user1")).thenReturn(testUser);
            when(userService.getUserInfo("user2")).thenReturn(replyToUser);
            when(commentRepository.findByIdAndNotDeleted("parent1"))
                    .thenReturn(Optional.of(parentComment));

            Comment savedReply = Comment.builder()
                    .id("reply1")
                    .discussionId("discussion1")
                    .content("回复内容")
                    .userId("user1")
                    .parentId("parent1")
                    .rootId("parent1")
                    .depth(1)
                    .path("parent1.abcd1234")
                    .replyToUserId("user2")
                    .replyToUsername("parentuser")
                    .deleted(false)
                    .build();

            when(commentRepository.save(any(Comment.class))).thenReturn(savedReply);

            // When
            CommentResponseDTO result = commentService.createComment("discussion1", replyRequestDTO, "user1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("回复内容");
            assertThat(result.getParentId()).isEqualTo("parent1");
            assertThat(result.getRootId()).isEqualTo("parent1");
            assertThat(result.getDepth()).isEqualTo(1);
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("讨论不存在时应抛出异常")
        void shouldThrowExceptionWhenDiscussionNotFound() {
            // Given
            when(discussionRepository.findByIdAndNotDeleted("discussion1"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                    commentService.createComment("discussion1", commentRequestDTO, "user1"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Discussion not found");
        }
    }

    @Nested
    @DisplayName("获取评论测试")
    class GetCommentTests {

        @Test
        @DisplayName("成功获取评论列表")
        void shouldGetCommentsSuccessfully() {
            // Given
            List<Comment> comments = List.of(testComment);
            Page<Comment> commentPage = new PageImpl<>(comments);

            when(discussionRepository.findByIdAndNotDeleted("discussion1"))
                    .thenReturn(Optional.of(testDiscussion));
            when(commentRepository.findByDiscussionId(eq("discussion1"), any(Pageable.class)))
                    .thenReturn(commentPage);

            // When
            Page<CommentResponseDTO> result = commentService.getCommentsByDiscussionId(
                    "discussion1", false, "createTime", "asc", 0, 10, "user1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getContent()).isEqualTo("测试评论");
        }

        @Test
        @DisplayName("讨论不存在时应抛出异常")
        void shouldThrowExceptionWhenDiscussionNotFoundForGet() {
            // Given
            when(discussionRepository.findByIdAndNotDeleted("discussion1"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                    commentService.getCommentsByDiscussionId(
                            "discussion1", false, "createTime", "asc", 0, 10, "user1"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Discussion not found");
        }
    }

    @Nested
    @DisplayName("删除评论测试")
    class DeleteCommentTests {

        @Test
        @DisplayName("成功删除自己的评论")
        void shouldDeleteOwnCommentSuccessfully() {
            // Given
            when(commentRepository.findByIdAndNotDeleted("comment1"))
                    .thenReturn(Optional.of(testComment));
            when(discussionRepository.findByIdAndNotDeleted("discussion1"))
                    .thenReturn(Optional.of(testDiscussion));
            when(commentRepository.countByDiscussionIdAndDeletedFalse("discussion1"))
                    .thenReturn(0L);

            // When
            commentService.deleteComment("comment1", "user1");

            // Then
            verify(commentRepository).save(argThat(comment -> 
                    comment.getDeleted() == true));
            verify(discussionRepository).save(any(Discussion.class));
        }

        @Test
        @DisplayName("删除他人评论应失败")
        void shouldFailToDeleteOthersComment() {
            // Given
            when(commentRepository.findByIdAndNotDeleted("comment1"))
                    .thenReturn(Optional.of(testComment));

            // When & Then
            assertThatThrownBy(() -> 
                    commentService.deleteComment("comment1", "user2"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("You are not authorized");
        }

        @Test
        @DisplayName("评论不存在时应抛出异常")
        void shouldThrowExceptionWhenCommentNotFoundForDelete() {
            // Given
            when(commentRepository.findByIdAndNotDeleted("comment1"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                    commentService.deleteComment("comment1", "user1"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Comment not found");
        }
    }

    @Nested
    @DisplayName("点赞功能测试")
    class ToggleLikeTests {

        @Test
        @DisplayName("成功点赞评论")
        void shouldLikeCommentSuccessfully() {
            // Given
            when(commentRepository.findByIdAndNotDeleted("comment1"))
                    .thenReturn(Optional.of(testComment));
            when(commentRepository.save(any(Comment.class)))
                    .thenReturn(testComment);

            // When
            CommentResponseDTO result = commentService.toggleLike("comment1", "user1");

            // Then
            assertThat(result).isNotNull();
            verify(commentRepository).save(argThat(comment -> 
                    comment.getLikedBy().contains("user1") && 
                    comment.getLikeCount() == 1));
        }

        @Test
        @DisplayName("成功取消点赞")
        void shouldUnlikeCommentSuccessfully() {
            // Given
            testComment.getLikedBy().add("user1");
            testComment.setLikeCount(1);
            
            when(commentRepository.findByIdAndNotDeleted("comment1"))
                    .thenReturn(Optional.of(testComment));
            when(commentRepository.save(any(Comment.class)))
                    .thenReturn(testComment);

            // When
            CommentResponseDTO result = commentService.toggleLike("comment1", "user1");

            // Then
            assertThat(result).isNotNull();
            verify(commentRepository).save(argThat(comment -> 
                    !comment.getLikedBy().contains("user1") && 
                    comment.getLikeCount() == 0));
        }

        @Test
        @DisplayName("评论不存在时点赞失败")
        void shouldFailToLikeNonExistentComment() {
            // Given
            when(commentRepository.findByIdAndNotDeleted("comment1"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                    commentService.toggleLike("comment1", "user1"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Comment not found");
        }
    }

    @Nested
    @DisplayName("举报评论测试")
    class ReportCommentTests {

        @Test
        @DisplayName("成功举报评论")
        void shouldReportCommentSuccessfully() {
            // Given
            ReportCommentDTO reportDTO = ReportCommentDTO.builder()
                    .reason("SPAM")
                    .details("垃圾评论")
                    .build();

            when(commentRepository.findByIdAndNotDeleted("comment1"))
                    .thenReturn(Optional.of(testComment));

            // When
            commentService.reportComment("comment1", reportDTO, "user1");

            // Then
            verify(commentRepository).save(argThat(comment -> 
                    "FLAGGED".equals(comment.getStatus())));
        }

        @Test
        @DisplayName("举报不存在的评论应失败")
        void shouldFailToReportNonExistentComment() {
            // Given
            ReportCommentDTO reportDTO = ReportCommentDTO.builder()
                    .reason("SPAM")
                    .details("垃圾评论")
                    .build();

            when(commentRepository.findByIdAndNotDeleted("comment1"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                    commentService.reportComment("comment1", reportDTO, "user1"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Comment not found");
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("父评论不存在时应抛出异常")
        void shouldThrowExceptionWhenParentCommentNotFound() {
            // Given
            CommentRequestDTO replyRequestDTO = CommentRequestDTO.builder()
                    .content("回复内容")
                    .parentId("nonexistent")
                    .build();

            when(discussionRepository.findByIdAndNotDeleted("discussion1"))
                    .thenReturn(Optional.of(testDiscussion));
            when(userService.getUserInfo("user1")).thenReturn(testUser);
            when(commentRepository.findByIdAndNotDeleted("nonexistent"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                    commentService.createComment("discussion1", replyRequestDTO, "user1"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Parent comment not found");
        }

        @Test
        @DisplayName("获取评论详情成功")
        void shouldGetCommentByIdSuccessfully() {
            // Given
            when(commentRepository.findByIdAndNotDeleted("comment1"))
                    .thenReturn(Optional.of(testComment));

            // When
            CommentResponseDTO result = commentService.getCommentById("comment1", "user1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("comment1");
            assertThat(result.getContent()).isEqualTo("测试评论");
        }

        @Test
        @DisplayName("获取不存在的评论应抛出异常")
        void shouldThrowExceptionWhenCommentNotFoundForGet() {
            // Given
            when(commentRepository.findByIdAndNotDeleted("comment1"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                    commentService.getCommentById("comment1", "user1"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Comment not found");
        }
    }
}
