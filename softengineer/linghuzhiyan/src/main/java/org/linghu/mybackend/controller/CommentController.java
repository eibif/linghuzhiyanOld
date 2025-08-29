package org.linghu.mybackend.controller;

import org.linghu.mybackend.dto.CommentRequestDTO;
import org.linghu.mybackend.dto.CommentResponseDTO;
import org.linghu.mybackend.dto.PageResult;
import org.linghu.mybackend.dto.ReportCommentDTO;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.service.CommentService;
import org.linghu.mybackend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "评论管理", description = "评论模块相关接口")
public class CommentController {
    
    private final CommentService commentService;
    private final UserService userService;
    @PostMapping("/api/discussions/{discussionId}/comments")
    @Operation(summary = "创建评论", description = "为指定的讨论创建新评论")
    public Result<CommentResponseDTO> createComment(
            @PathVariable String discussionId,
            @RequestBody CommentRequestDTO requestDTO) {
        
        String userId = userService.getCurrentUserId();
        CommentResponseDTO responseDTO = commentService.createComment(discussionId, requestDTO, userId);
        return Result.success(responseDTO);
    }
    @GetMapping("/api/discussions/{discussionId}/comments")
    @Operation(summary = "获取讨论的评论列表", description = "分页获取指定讨论的评论列表")
    public Result<PageResult<CommentResponseDTO>> getCommentsByDiscussionId(
            @PathVariable String discussionId,
            @RequestParam(required = false, defaultValue = "false") boolean rootOnly,
            @RequestParam(required = false, defaultValue = "createTime") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String order,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        String currentUserId = null;
        try {
            currentUserId = userService.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户不影响浏览评论
        }
        
        Page<CommentResponseDTO> commentsPage = commentService.getCommentsByDiscussionId(
                discussionId, rootOnly, sortBy, order, page, size, currentUserId);
        
        PageResult<CommentResponseDTO> pageResult = PageResult.of(
            commentsPage.getContent(),
            commentsPage.getTotalElements(),
            page,
            size
        );
        
        return Result.success(pageResult);
    }
    @GetMapping("/api/comments/{commentId}/replies")
    @Operation(summary = "获取评论的回复", description = "分页获取指定评论的回复列表")
    public Result<PageResult<CommentResponseDTO>> getRepliesByCommentId(
            @PathVariable String commentId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        String currentUserId = null;
        try {
            currentUserId = userService.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户不影响浏览评论
        }
        
        Page<CommentResponseDTO> replies = commentService.getRepliesByCommentId(commentId, page, size, currentUserId);
        
        PageResult<CommentResponseDTO> pageResult = PageResult.of(
            replies.getContent(),
            replies.getTotalElements(),
            page,
            size
        );
        
        return Result.success(pageResult);
    }
    @DeleteMapping("/api/comments/{commentId}")
    @Operation(summary = "删除评论", description = "删除指定ID的评论")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> deleteComment(@PathVariable String commentId) {
        String userId = userService.getCurrentUserId();
        commentService.deleteComment(commentId, userId);
        return Result.success();
    }
    @PostMapping("/api/comments/{commentId}/like")
    @Operation(summary = "点赞/取消点赞评论", description = "对评论进行点赞或取消点赞")
    public Result<CommentResponseDTO> toggleLike(@PathVariable String commentId) {
        String userId = userService.getCurrentUserId();
        CommentResponseDTO comment = commentService.toggleLike(commentId, userId);
        return Result.success(comment);
    }
      @GetMapping("/api/comments/{commentId}")
    @Operation(summary = "获取评论详情", description = "根据ID获取评论详情")
    public Result<CommentResponseDTO> getCommentById(@PathVariable String commentId) {
        String currentUserId = null;
        try {
            currentUserId = userService.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户不影响浏览评论
        }
        
        CommentResponseDTO comment = commentService.getCommentById(commentId, currentUserId);
        return Result.success(comment);
    }
      @PostMapping("/api/comments/{commentId}/report")
    @Operation(summary = "举报评论", description = "举报不当评论内容")
    public Result<Void> reportComment(
            @PathVariable String commentId,
            @RequestBody ReportCommentDTO reportDTO) {
        
        String userId = userService.getCurrentUserId();
        commentService.reportComment(commentId, reportDTO, userId);
        return Result.success();
    }
}
