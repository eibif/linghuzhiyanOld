package org.linghu.mybackend.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.linghu.mybackend.domain.Attachment;
import org.linghu.mybackend.domain.Discussion;
import org.linghu.mybackend.domain.RichContent;
import org.linghu.mybackend.dto.AttachmentDTO;
import org.linghu.mybackend.dto.DiscussionRequestDTO;
import org.linghu.mybackend.dto.DiscussionResponseDTO;
import org.linghu.mybackend.dto.PriorityRequestDTO;
import org.linghu.mybackend.dto.ReviewRequestDTO;
import org.linghu.mybackend.dto.RichContentDTO;
import org.linghu.mybackend.dto.UserDTO;
import org.linghu.mybackend.exception.ResourceNotFoundException;
import org.linghu.mybackend.exception.UnauthorizedException;
import org.linghu.mybackend.repository.DiscussionRepository;
import org.linghu.mybackend.service.DiscussionService;
import org.linghu.mybackend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class DiscussionServiceImpl implements DiscussionService {

    private final DiscussionRepository discussionRepository;
    private final UserService userService;
    private final MongoTemplate mongoTemplate;

    @Override
    public DiscussionResponseDTO createDiscussion(DiscussionRequestDTO requestDTO, String userId) {
        UserDTO userInfo = userService.getUserInfo(userId);

        Discussion discussion = Discussion.builder()
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .richContent(RichContent.builder()
                        .html(requestDTO.getRichContent().getHtml())
                        .delta(requestDTO.getRichContent().getDelta())
                        .build())
                .userId(userId)
                .username(userInfo.getUsername())
                .userAvatar(userInfo.getAvatar())
                .tags(requestDTO.getTags())
                .experimentId(requestDTO.getExperimentId())
                .status("PENDING") // 默认等待审核
                .priority(0)
                .viewCount(0L)
                .commentCount(0L)
                .likeCount(0L)
                .likedBy(new ArrayList<>())
                .lastActivityTime(LocalDateTime.now())
                .attachments(mapAttachmentDTOsToAttachments(requestDTO.getAttachments()))
                .deleted(false)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        Discussion savedDiscussion = discussionRepository.save(discussion);
        return mapDiscussionToResponseDTO(savedDiscussion, userId);
    }

    @Override
    public Page<DiscussionResponseDTO> getDiscussions(
            String[] tags,
            String experimentId,
            String userId,
            String status,
            String keyword,
            String sortBy,
            String order,
            int page,
            int size,
            String currentUserId,
            String highestRole) {

        Sort sort = createSort(sortBy, order);
        Pageable pageable = PageRequest.of(page, size, sort);

    // removed unused local variable "discussions"

        // 构建基础查询
        Query query = new Query();
        query.addCriteria(Criteria.where("deleted").is(false));

        // 添加过滤条件
        if (status != null && !status.isEmpty()) {
            // 显式指定状态则按指定状态过滤
            query.addCriteria(Criteria.where("status").is(status));
        } else {
            // 未指定状态时的默认展示规则
            if ("ROLE_STUDENT".equals(highestRole)) {
                // 学生：默认看到所有已审核通过的 + 自己创建的任意状态讨论
                if (userId != null && !userId.isEmpty()) {
                    if (currentUserId != null && userId.equals(currentUserId)) {
                        // 查询自己时：不过滤状态（展示自己全部状态）
                    } else {
                        // 查询他人时：只展示已审核通过的
                        query.addCriteria(Criteria.where("status").is("APPROVED"));
                    }
                } else if (currentUserId != null) {
                    // 未指定 userId：展示 (status=APPROVED) OR (userId=当前学生)
                    query.addCriteria(new Criteria().orOperator(
                            Criteria.where("status").is("APPROVED"),
                            Criteria.where("userId").is(currentUserId)
                    ));
                } else {
                    // 理论上学生应有 currentUserId；兜底为只展示已审核通过的
                    query.addCriteria(Criteria.where("status").is("APPROVED"));
                }
            } else {
                // 非学生（管理员/老师/助教）：默认可见全部状态
                query.addCriteria(Criteria.where("status").in(Arrays.asList("APPROVED", "PENDING", "REJECTED")));
            }
        }

        if (tags != null && tags.length > 0) {
            query.addCriteria(Criteria.where("tags").in(Arrays.asList(tags)));
        }

        if (experimentId != null && !experimentId.isEmpty()) {
            query.addCriteria(Criteria.where("experimentId").is(experimentId));
        }

        if (userId != null && !userId.isEmpty()) {
            query.addCriteria(Criteria.where("userId").is(userId));
        }

        if (keyword != null && !keyword.isEmpty()) {
            // 使用文本搜索
            query.addCriteria(Criteria.where("$text").is(keyword));
        }

        // 执行查询
        long total = mongoTemplate.count(query, Discussion.class);
        query.with(pageable);
        List<Discussion> content = mongoTemplate.find(query, Discussion.class);

        Page<Discussion> discussionsPage = new org.springframework.data.domain.PageImpl<>(content, pageable, total);

        return discussionsPage.map(discussion -> mapDiscussionToResponseDTO(discussion, currentUserId));
    }

    @Override
    public DiscussionResponseDTO getDiscussionById(String id, String currentUserId) {
        Discussion discussion = discussionRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found with id: " + id));

        // 增加浏览次数
        incrementViewCount(id);

        return mapDiscussionToResponseDTO(discussion, currentUserId);
    }

    @Override
    public DiscussionResponseDTO updateDiscussion(String id, DiscussionRequestDTO requestDTO, String userId) {
        Discussion discussion = discussionRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found with id: " + id));

        // 验证是否是创建者
        if (!discussion.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this discussion");
        }

        // 更新内容
        discussion.setTitle(requestDTO.getTitle());
        discussion.setContent(requestDTO.getContent());
        discussion.setRichContent(RichContent.builder()
                .html(requestDTO.getRichContent().getHtml())
                .delta(requestDTO.getRichContent().getDelta())
                .build());
        discussion.setTags(requestDTO.getTags());
        discussion.setUpdateTime(LocalDateTime.now());
        discussion.setLastActivityTime(LocalDateTime.now());
        // 如果有附件更新
        if (requestDTO.getAttachments() != null) {
            discussion.setAttachments(mapAttachmentDTOsToAttachments(requestDTO.getAttachments()));
        }

        // 更新后重新设为待审核状态
        discussion.setStatus("PENDING");

        Discussion updatedDiscussion = discussionRepository.save(discussion);
        return mapDiscussionToResponseDTO(updatedDiscussion, userId);
    }

    @Override
    public boolean deleteDiscussion(String id, String userId) {
        // 幂等删除：若不存在或已删除，直接视为成功
        Discussion existing = mongoTemplate.findById(id, Discussion.class);
        if (existing == null) {
            return true;
        }
        if (Boolean.TRUE.equals(existing.getDeleted())) {
            return true;
        }

        // 验证是否是创建者（兼容历史数据 userId 存用户名的情况）
        boolean isOwner = false;
        if (userId != null) {
            if (userId.equals(existing.getUserId()) || userId.equals(existing.getUsername())) {
                isOwner = true;
            }
        }
        if (!isOwner) {
            throw new UnauthorizedException("You are not authorized to delete this discussion");
        }

        // 软删除
        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        discussionRepository.save(existing);
        return true;
    }

    @Override
    public DiscussionResponseDTO reviewDiscussion(String id, ReviewRequestDTO requestDTO, String reviewerId) {
        Discussion discussion = discussionRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found with id: " + id));

        // 更新审核状态
        discussion.setStatus(requestDTO.getStatus());

        if ("REJECTED".equals(requestDTO.getStatus())) {
            discussion.setRejectionReason(requestDTO.getRejectionReason());
        } else if ("APPROVED".equals(requestDTO.getStatus())) {
            discussion.setApprovedTime(LocalDateTime.now());
            discussion.setApprovedBy(reviewerId);
        }

        discussion.setUpdateTime(LocalDateTime.now());

        Discussion reviewedDiscussion = discussionRepository.save(discussion);
        return mapDiscussionToResponseDTO(reviewedDiscussion, reviewerId);
    }

    @Override
    public DiscussionResponseDTO updatePriority(String id, PriorityRequestDTO requestDTO, String userId) {
        Discussion discussion = discussionRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found with id: " + id));

        discussion.setPriority(requestDTO.getPriority());
        discussion.setUpdateTime(LocalDateTime.now());

        Discussion updatedDiscussion = discussionRepository.save(discussion);
        return mapDiscussionToResponseDTO(updatedDiscussion, userId);
    }

    @Override
    public DiscussionResponseDTO toggleLike(String id, String userId) {
        Discussion discussion = discussionRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found with id: " + id));

        List<String> likedBy = discussion.getLikedBy();

        if (likedBy.contains(userId)) {
            // 取消点赞
            likedBy.remove(userId);
            discussion.setLikeCount(discussion.getLikeCount() - 1);
        } else {
            // 添加点赞
            likedBy.add(userId);
            discussion.setLikeCount(discussion.getLikeCount() + 1);
        }

        discussion.setLikedBy(likedBy);
        discussion.setUpdateTime(LocalDateTime.now());
        discussion.setLastActivityTime(LocalDateTime.now());

        Discussion updatedDiscussion = discussionRepository.save(discussion);
        return mapDiscussionToResponseDTO(updatedDiscussion, userId);
    }

    @Override
    public void incrementViewCount(String id) {
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(id)),
                update,
                Discussion.class);
    }

    @Override
    public java.util.List<DiscussionResponseDTO> getDiscussionsByExperimentIds(java.util.List<String> experimentIds) {
        if (experimentIds == null || experimentIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // 查询所有未删除且experimentId在列表中的讨论
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query();
        query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("deleted").is(false));
        query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("experimentId").in(experimentIds));
        java.util.List<org.linghu.mybackend.domain.Discussion> discussions = mongoTemplate.find(query, org.linghu.mybackend.domain.Discussion.class);
        return discussions.stream().map(d -> mapDiscussionToResponseDTO(d, null)).collect(java.util.stream.Collectors.toList());
    }

    // 辅助方法

    private Sort createSort(String sortBy, String order) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "lastActivityTime"; // 默认按最后活动时间排序
        }

        if (order == null || order.isEmpty()) {
            order = "desc"; // 默认降序
        }

        // 映射API参数到字段名称
        String fieldName = switch (sortBy) {
            case "createTime" -> "createTime";
            case "lastActivityTime" -> "lastActivityTime";
            case "likeCount" -> "likeCount";
            case "commentCount" -> "commentCount";
            case "viewCount" -> "viewCount";
            default -> "lastActivityTime";
        };

        // 创建排序
        Sort sort;
        if ("asc".equals(order.toLowerCase())) {
            sort = Sort.by(fieldName).ascending();
        } else {
            sort = Sort.by(fieldName).descending();
        }

        // 置顶内容总是优先
        return Sort.by(Sort.Order.desc("priority")).and(sort);
    }

    private List<Attachment> mapAttachmentDTOsToAttachments(List<AttachmentDTO> attachmentDTOs) {
        if (attachmentDTOs == null) {
            return new ArrayList<>();
        }

        return attachmentDTOs.stream()
                .map(dto -> Attachment.builder()
                        .url(dto.getUrl())
                        .type(dto.getType())
                        .name(dto.getName())
                        .size(dto.getSize())
                        .thumbnailUrl(dto.getThumbnailUrl())
                        .build())
                .collect(Collectors.toList());
    }

    private List<AttachmentDTO> mapAttachmentsToAttachmentDTOs(List<Attachment> attachments) {
        if (attachments == null) {
            return new ArrayList<>();
        }

        return attachments.stream()
                .map(attachment -> AttachmentDTO.builder()
                        .url(attachment.getUrl())
                        .type(attachment.getType())
                        .name(attachment.getName())
                        .size(attachment.getSize())
                        .thumbnailUrl(attachment.getThumbnailUrl())
                        .build())
                .collect(Collectors.toList());
    }

    private DiscussionResponseDTO mapDiscussionToResponseDTO(Discussion discussion, String currentUserId) {
        return DiscussionResponseDTO.builder()
                .id(discussion.getId())
                .title(discussion.getTitle())
                .content(discussion.getContent())
                .richContent(RichContentDTO.builder()
                        .html(discussion.getRichContent() != null ? discussion.getRichContent().getHtml() : null)
                        .delta(discussion.getRichContent() != null ? discussion.getRichContent().getDelta() : null)
                        .build())
                .userId(discussion.getUserId())
                .username(discussion.getUsername())
                .userAvatar(discussion.getUserAvatar())
                .tags(discussion.getTags())
                .experimentId(discussion.getExperimentId())
                .status(discussion.getStatus())
                .rejectionReason(discussion.getRejectionReason())
                .priority(discussion.getPriority())
                .viewCount(discussion.getViewCount())
                .commentCount(discussion.getCommentCount())
                .likeCount(discussion.getLikeCount())
                .isLiked(discussion.getLikedBy() != null && discussion.getLikedBy().contains(currentUserId))
                .lastCommentTime(discussion.getLastCommentTime())
                .lastActivityTime(discussion.getLastActivityTime())
                .attachments(mapAttachmentsToAttachmentDTOs(discussion.getAttachments()))
                .createTime(discussion.getCreateTime())
                .updateTime(discussion.getUpdateTime())
                .approvedTime(discussion.getApprovedTime())
                .build();
    }
}
