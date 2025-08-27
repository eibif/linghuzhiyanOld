package org.linghu.mybackend.controller;

import org.linghu.mybackend.dto.DiscussionRequestDTO;
import org.linghu.mybackend.dto.DiscussionResponseDTO;
import org.linghu.mybackend.dto.PageResult;
import org.linghu.mybackend.dto.PriorityRequestDTO;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.dto.ReviewRequestDTO;
import org.linghu.mybackend.service.DiscussionService;
import org.linghu.mybackend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
@Tag(name = "讨论管理", description = "讨论模块相关接口")
public class DiscussionController {

    private final DiscussionService discussionService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "创建讨论", description = "创建一个新的讨论主题")
    public Result<DiscussionResponseDTO> createDiscussion(
            @RequestBody DiscussionRequestDTO requestDTO) {
        String userId = userService.getCurrentUserId();
        DiscussionResponseDTO responseDTO = discussionService.createDiscussion(requestDTO, userId);
        return Result.success(responseDTO);
    }

    @GetMapping
    @Operation(summary = "获取讨论列表", description = "分页获取讨论列表，支持多种过滤和排序")
    public Result<PageResult<DiscussionResponseDTO>> getDiscussions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String experimentId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "lastActivityTime") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String order,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        // 获取用户的最高权限角色（按优先级：ADMIN > TEACHER > ASSISTANT > STUDENT）
        String currentUserId = null;
        String highestRole = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .filter(role -> role.startsWith("ROLE_"))
                .sorted((role1, role2) -> {
                    // 定义权限优先级：数字越小优先级越高
                    int priority1 = getRolePriority(role1);
                    int priority2 = getRolePriority(role2);
                    return Integer.compare(priority1, priority2);
                })
                .findFirst()
                .orElse(null);

        if (highestRole != null) {
            currentUserId = userDetails.getUsername();
        }
        try {
            currentUserId = userService.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户不影响浏览讨论
        }

        String[] tagArray = tags != null ? tags.split(",") : null;

        Page<DiscussionResponseDTO> discussionsPage = discussionService.getDiscussions(
                tagArray, experimentId, userId, status, keyword, sortBy, order, page, size, currentUserId,highestRole);

        PageResult<DiscussionResponseDTO> pageResult = PageResult.of(
                discussionsPage.getContent(),
                discussionsPage.getTotalElements(),
                page,
                size);

        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取讨论详情", description = "根据ID获取讨论详情")
    public Result<DiscussionResponseDTO> getDiscussionById(@PathVariable String id) {
        String currentUserId = null;
        try {
            currentUserId = userService.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户不影响浏览讨论
        }

        DiscussionResponseDTO discussion = discussionService.getDiscussionById(id, currentUserId);
        return Result.success(discussion);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新讨论", description = "更新讨论内容")
    public Result<DiscussionResponseDTO> updateDiscussion(
            @PathVariable String id,
            @RequestBody DiscussionRequestDTO requestDTO) {

        String userId = userService.getCurrentUserId();
        DiscussionResponseDTO updatedDiscussion = discussionService.updateDiscussion(id, requestDTO, userId);
        return Result.success(updatedDiscussion);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除讨论", description = "删除指定ID的讨论")
    public Result<Boolean> deleteDiscussion(@PathVariable String id) {
        String userId = userService.getCurrentUserId();
        boolean deleted = discussionService.deleteDiscussion(id, userId);
        return Result.success(deleted);
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "审核讨论", description = "审核讨论内容")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_TEACHER','ROLE_ASSISTANT')")
    public Result<DiscussionResponseDTO> reviewDiscussion(
            @PathVariable String id,
            @RequestBody ReviewRequestDTO requestDTO) {

        String reviewerId = userService.getCurrentUserId();
        DiscussionResponseDTO reviewedDiscussion = discussionService.reviewDiscussion(id, requestDTO, reviewerId);
        return Result.success(reviewedDiscussion);
    }

    @PutMapping("/{id}/priority")
    @Operation(summary = "设置讨论优先级", description = "设置讨论的优先级(置顶)")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_TEACHER','ROLE_ASSISTANT')")
    public Result<DiscussionResponseDTO> updatePriority(
            @PathVariable String id,
            @RequestBody PriorityRequestDTO requestDTO) {

        String userId = userService.getCurrentUserId();
        DiscussionResponseDTO updatedDiscussion = discussionService.updatePriority(id, requestDTO, userId);
        return Result.success(updatedDiscussion);
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "点赞/取消点赞讨论", description = "对讨论进行点赞或取消点赞")
    public Result<DiscussionResponseDTO> toggleLike(@PathVariable String id) {
        String userId = userService.getCurrentUserId();
        DiscussionResponseDTO discussion = discussionService.toggleLike(id, userId);
        return Result.success(discussion);
    }

    /**
     * 获取角色优先级，数字越小优先级越高
     * ADMIN = 1, TEACHER = 2, ASSISTANT = 3, STUDENT = 4, 其他 = 999
     */
    private int getRolePriority(String role) {
        switch (role) {
            case "ROLE_ADMIN":
                return 1;
            case "ROLE_TEACHER":
                return 2;
            case "ROLE_ASSISTANT":
                return 3;
            case "ROLE_STUDENT":
                return 4;
            default:
                return 999; // 未知角色给最低优先级
        }
    }
}
