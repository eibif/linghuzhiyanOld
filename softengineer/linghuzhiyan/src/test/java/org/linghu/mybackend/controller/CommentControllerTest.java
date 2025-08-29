package org.linghu.mybackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.controller.CommentController;
import org.linghu.mybackend.dto.CommentRequestDTO;
import org.linghu.mybackend.dto.CommentResponseDTO;
import org.linghu.mybackend.dto.PageResult;
import org.linghu.mybackend.dto.ReportCommentDTO;
import org.linghu.mybackend.service.CommentService;
import org.linghu.mybackend.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentController 单元测试")
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentController commentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private CommentRequestDTO validCommentRequest;
    private CommentResponseDTO sampleCommentResponse;
    private ReportCommentDTO validReportRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
        objectMapper = new ObjectMapper();

        // 准备测试数据
        validCommentRequest = CommentRequestDTO.builder()
                .content("这是一个测试评论内容")
                .parentId(null)
                .replyToUserId(null)
                .build();

        sampleCommentResponse = CommentResponseDTO.builder()
                .id("comment-1")
                .discussionId("discussion-1")
                .content("这是一个测试评论内容")
                .userId("user-1")
                .username("testuser")
                .userAvatar("avatar.jpg")
                .likeCount(5)
                .isLiked(false)
                .status("APPROVED")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .replies(Collections.emptyList())
                .build();

        validReportRequest = ReportCommentDTO.builder()
                .reason("不当内容")
                .details("包含不当言论")
                .build();
    }

    @Nested
    @DisplayName("创建评论测试")
    class CreateCommentTests {

        @Test
        @DisplayName("成功创建评论")
        void createComment_Success() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");
            when(commentService.createComment(eq("discussion-1"), any(CommentRequestDTO.class), eq("user-1")))
                    .thenReturn(sampleCommentResponse);

            // Act & Assert
            mockMvc.perform(post("/api/discussions/{discussionId}/comments", "discussion-1")
                            .with(user(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCommentRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value("comment-1"))
                    .andExpect(jsonPath("$.data.content").value("这是一个测试评论内容"));

            verify(commentService).createComment(eq("discussion-1"), any(CommentRequestDTO.class), eq("user-1"));
        }

        @Test
        @DisplayName("创建回复评论成功")
        void createReplyComment_Success() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            CommentRequestDTO replyRequest = CommentRequestDTO.builder()
                    .content("这是一个回复评论")
                    .parentId("parent-comment-1")
                    .replyToUserId("replied-user-1")
                    .build();

            CommentResponseDTO replyResponse = CommentResponseDTO.builder()
                    .id("reply-comment-1")
                    .discussionId("discussion-1")
                    .content("这是一个回复评论")
                    .userId("user-1")
                    .username("testuser")
                    .parentId("parent-comment-1")
                    .replyToUserId("replied-user-1")
                    .likeCount(0)
                    .isLiked(false)
                    .status("APPROVED")
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .replies(Collections.emptyList())
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");
            when(commentService.createComment(eq("discussion-1"), any(CommentRequestDTO.class), eq("user-1")))
                    .thenReturn(replyResponse);

            // Act & Assert
            mockMvc.perform(post("/api/discussions/{discussionId}/comments", "discussion-1")
                            .with(user(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(replyRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.parentId").value("parent-comment-1"));
        }

        @Test
        @DisplayName("创建评论失败 - 内容为空")
        void createComment_EmptyContent_Failure() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            CommentRequestDTO invalidRequest = CommentRequestDTO.builder()
                    .content("")
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");

            // Act & Assert
            mockMvc.perform(post("/api/discussions/{discussionId}/comments", "discussion-1")
                            .with(user(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(commentService, never()).createComment(anyString(), any(CommentRequestDTO.class), anyString());
        }

        @Test
        @DisplayName("创建评论失败 - 服务异常")
        void createComment_ServiceException_Failure() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");
            when(commentService.createComment(anyString(), any(CommentRequestDTO.class), anyString()))
                    .thenThrow(new RuntimeException("创建评论失败"));

            // Act & Assert
            mockMvc.perform(post("/api/discussions/{discussionId}/comments", "discussion-1")
                            .with(user(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCommentRequest)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("获取评论列表测试")
    class GetCommentsTests {

        @Test
        @DisplayName("获取讨论评论列表成功")
        void getCommentsByDiscussionId_Success() throws Exception {
            // Arrange
            List<CommentResponseDTO> comments = Collections.singletonList(sampleCommentResponse);
            Page<CommentResponseDTO> commentsPage = new PageImpl<>(comments, PageRequest.of(0, 10), 1);

            when(commentService.getCommentsByDiscussionId(
                    eq("discussion-1"), eq(false), eq("createTime"), eq("asc"), 
                    eq(0), eq(10), isNull()))
                    .thenReturn(commentsPage);

            // Act & Assert
            mockMvc.perform(get("/api/discussions/{discussionId}/comments", "discussion-1")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.list").isArray())
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.list[0].id").value("comment-1"));

            verify(commentService).getCommentsByDiscussionId(
                    eq("discussion-1"), eq(false), eq("createTime"), eq("asc"), 
                    eq(0), eq(10), isNull());
        }

        @Test
        @DisplayName("获取评论回复列表成功")
        void getRepliesByCommentId_Success() throws Exception {
            // Arrange
            List<CommentResponseDTO> replies = Collections.singletonList(sampleCommentResponse);
            Page<CommentResponseDTO> repliesPage = new PageImpl<>(replies, PageRequest.of(0, 10), 1);

            when(commentService.getRepliesByCommentId(eq("comment-1"), eq(0), eq(10), isNull()))
                    .thenReturn(repliesPage);

            // Act & Assert
            mockMvc.perform(get("/api/comments/{commentId}/replies", "comment-1")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.list").isArray())
                    .andExpect(jsonPath("$.data.total").value(1));

            verify(commentService).getRepliesByCommentId(eq("comment-1"), eq(0), eq(10), isNull());
        }

        @Test
        @DisplayName("已登录用户获取评论列表")
        void getCommentsByDiscussionId_AuthenticatedUser_Success() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            List<CommentResponseDTO> comments = Collections.singletonList(sampleCommentResponse);
            Page<CommentResponseDTO> commentsPage = new PageImpl<>(comments, PageRequest.of(0, 10), 1);

            when(userService.getCurrentUserId()).thenReturn("user-1");
            when(commentService.getCommentsByDiscussionId(
                    eq("discussion-1"), eq(false), eq("createTime"), eq("asc"), 
                    eq(0), eq(10), eq("user-1")))
                    .thenReturn(commentsPage);

            // Act & Assert
            mockMvc.perform(get("/api/discussions/{discussionId}/comments", "discussion-1")
                            .with(user(user))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(commentService).getCommentsByDiscussionId(
                    eq("discussion-1"), eq(false), eq("createTime"), eq("asc"), 
                    eq(0), eq(10), eq("user-1"));
        }
    }

    @Nested
    @DisplayName("获取评论详情测试")
    class GetCommentDetailsTests {

        @Test
        @DisplayName("获取评论详情成功")
        void getCommentById_Success() throws Exception {
            // Arrange
            when(commentService.getCommentById(eq("comment-1"), isNull()))
                    .thenReturn(sampleCommentResponse);

            // Act & Assert
            mockMvc.perform(get("/api/comments/{commentId}", "comment-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value("comment-1"))
                    .andExpect(jsonPath("$.data.content").value("这是一个测试评论内容"));

            verify(commentService).getCommentById(eq("comment-1"), isNull());
        }

        @Test
        @DisplayName("已登录用户获取评论详情")
        void getCommentById_AuthenticatedUser_Success() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");
            when(commentService.getCommentById(eq("comment-1"), eq("user-1")))
                    .thenReturn(sampleCommentResponse);

            // Act & Assert
            mockMvc.perform(get("/api/comments/{commentId}", "comment-1")
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value("comment-1"));

            verify(commentService).getCommentById(eq("comment-1"), eq("user-1"));
        }

        @Test
        @DisplayName("获取评论详情失败 - 评论不存在")
        void getCommentById_NotFound_Failure() throws Exception {
            // Arrange
            when(commentService.getCommentById(anyString(), any()))
                    .thenThrow(new RuntimeException("评论不存在"));

            // Act & Assert
            mockMvc.perform(get("/api/comments/{commentId}", "non-existent"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("删除评论测试")
    class DeleteCommentTests {

        @Test
        @DisplayName("管理员删除评论成功")
        void deleteComment_AdminUser_Success() throws Exception {
            // Arrange
            UserDetails adminUser = User.builder()
                    .username("admin")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            when(userService.getCurrentUserId()).thenReturn("admin");
            doNothing().when(commentService).deleteComment(eq("comment-1"), eq("admin"));

            // Act & Assert
            mockMvc.perform(delete("/api/comments/{commentId}", "comment-1")
                            .with(user(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(commentService).deleteComment(eq("comment-1"), eq("admin"));
        }

        @Test
        @DisplayName("教师删除评论成功")
        void deleteComment_TeacherUser_Success() throws Exception {
            // Arrange
            UserDetails teacherUser = User.builder()
                    .username("teacher")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
                    .build();

            when(userService.getCurrentUserId()).thenReturn("teacher");
            doNothing().when(commentService).deleteComment(eq("comment-1"), eq("teacher"));

            // Act & Assert
            mockMvc.perform(delete("/api/comments/{commentId}", "comment-1")
                            .with(user(teacherUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(commentService).deleteComment(eq("comment-1"), eq("teacher"));
        }

        @Test
        @DisplayName("学生删除评论失败 - 权限不足")
        void deleteComment_StudentUser_Forbidden() throws Exception {
            // Arrange
            UserDetails studentUser = User.builder()
                    .username("student")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            // Act & Assert
            mockMvc.perform(delete("/api/comments/{commentId}", "comment-1")
                            .with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(commentService, never()).deleteComment(anyString(), anyString());
        }

        @Test
        @DisplayName("删除评论失败 - 评论不存在")
        void deleteComment_NotFound_Failure() throws Exception {
            // Arrange
            UserDetails adminUser = User.builder()
                    .username("admin")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            when(userService.getCurrentUserId()).thenReturn("admin");
            doThrow(new RuntimeException("评论不存在"))
                    .when(commentService).deleteComment(anyString(), anyString());

            // Act & Assert
            mockMvc.perform(delete("/api/comments/{commentId}", "non-existent")
                            .with(user(adminUser)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("点赞评论测试")
    class ToggleLikeTests {

        @Test
        @DisplayName("点赞评论成功")
        void toggleLike_Success() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            CommentResponseDTO likedComment = CommentResponseDTO.builder()
                    .id("comment-1")
                    .discussionId("discussion-1")
                    .content("这是一个测试评论内容")
                    .userId("user-1")
                    .username("testuser")
                    .userAvatar("avatar.jpg")
                    .isLiked(true)
                    .likeCount(6)
                    .status("APPROVED")
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .replies(Collections.emptyList())
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");
            when(commentService.toggleLike(eq("comment-1"), eq("user-1")))
                    .thenReturn(likedComment);

            // Act & Assert
            mockMvc.perform(post("/api/comments/{commentId}/like", "comment-1")
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.isLiked").value(true))
                    .andExpect(jsonPath("$.data.likeCount").value(6));

            verify(commentService).toggleLike(eq("comment-1"), eq("user-1"));
        }

        @Test
        @DisplayName("取消点赞评论成功")
        void toggleLike_Unlike_Success() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            CommentResponseDTO unlikedComment = CommentResponseDTO.builder()
                    .id("comment-1")
                    .discussionId("discussion-1")
                    .content("这是一个测试评论内容")
                    .userId("user-1")
                    .username("testuser")
                    .userAvatar("avatar.jpg")
                    .isLiked(false)
                    .likeCount(4)
                    .status("APPROVED")
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .replies(Collections.emptyList())
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");
            when(commentService.toggleLike(eq("comment-1"), eq("user-1")))
                    .thenReturn(unlikedComment);

            // Act & Assert
            mockMvc.perform(post("/api/comments/{commentId}/like", "comment-1")
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.isLiked").value(false))
                    .andExpect(jsonPath("$.data.likeCount").value(4));

            verify(commentService).toggleLike(eq("comment-1"), eq("user-1"));
        }

        @Test
        @DisplayName("点赞失败 - 评论不存在")
        void toggleLike_NotFound_Failure() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");
            when(commentService.toggleLike(anyString(), anyString()))
                    .thenThrow(new RuntimeException("评论不存在"));

            // Act & Assert
            mockMvc.perform(post("/api/comments/{commentId}/like", "non-existent")
                            .with(user(user)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("举报评论测试")
    class ReportCommentTests {

        @Test
        @DisplayName("举报评论成功")
        void reportComment_Success() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");
            doNothing().when(commentService).reportComment(eq("comment-1"), any(ReportCommentDTO.class), eq("user-1"));

            // Act & Assert
            mockMvc.perform(post("/api/comments/{commentId}/report", "comment-1")
                            .with(user(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validReportRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(commentService).reportComment(eq("comment-1"), any(ReportCommentDTO.class), eq("user-1"));
        }

        @Test
        @DisplayName("举报失败 - 举报原因为空")
        void reportComment_EmptyReason_Failure() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            ReportCommentDTO invalidRequest = ReportCommentDTO.builder()
                    .reason("")
                    .details("详情")
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");

            // Act & Assert
            mockMvc.perform(post("/api/comments/{commentId}/report", "comment-1")
                            .with(user(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(commentService, never()).reportComment(anyString(), any(ReportCommentDTO.class), anyString());
        }

        @Test
        @DisplayName("举报失败 - 服务异常")
        void reportComment_ServiceException_Failure() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");
            doThrow(new RuntimeException("举报失败"))
                    .when(commentService).reportComment(anyString(), any(ReportCommentDTO.class), anyString());

            // Act & Assert
            mockMvc.perform(post("/api/comments/{commentId}/report", "comment-1")
                            .with(user(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validReportRequest)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("处理空指针异常")
        void handleNullPointerException() throws Exception {
            // Arrange
            when(commentService.getCommentById(anyString(), any()))
                    .thenThrow(new NullPointerException("Null pointer"));

            // Act & Assert
            mockMvc.perform(get("/api/comments/comment-1"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("处理非法参数异常")
        void handleIllegalArgumentException() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("user-1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            when(userService.getCurrentUserId()).thenReturn("user-1");
            when(commentService.toggleLike(anyString(), anyString()))
                    .thenThrow(new IllegalArgumentException("Invalid comment ID"));

            // Act & Assert
            mockMvc.perform(post("/api/comments/invalid-id/like")
                            .with(user(user)))
                    .andExpect(status().isInternalServerError());
        }
    }
}
