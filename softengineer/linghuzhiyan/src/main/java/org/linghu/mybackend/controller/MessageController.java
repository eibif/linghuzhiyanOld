package org.linghu.mybackend.controller;

import java.util.List;

import org.linghu.mybackend.dto.MessageDTO;
import org.linghu.mybackend.dto.MessageRequestDTO;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.dto.SenderInfoDTO;
import org.linghu.mybackend.service.MessageService;
import org.linghu.mybackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 消息通知API控制器
 */
@RestController
@RequestMapping("/api/messages")
@Tag(name = "通知管理", description = "通知管理相关API")
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    @Autowired
    public MessageController(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "创建通知", description = "创建一条新的消息通知")
    public Result<MessageDTO> createMessage(@RequestBody MessageRequestDTO messageRequestDTO, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new RuntimeException("未登录用户无法发送消息");
        }
        var authorities = userDetails.getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isTeacher = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        boolean isAssistant = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ASSISTANT"));
        boolean isStudent = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        // receiver为用户名，先查用户实体再查id
        var receiverUserDTO = userService.getUserByUsername(messageRequestDTO.getReceiver());
        if (receiverUserDTO == null || receiverUserDTO.getId() == null) {
            throw new RuntimeException("接收者用户不存在");
        }
        String receiverId = receiverUserDTO.getId();
        java.util.Set<String> receiverRoles = userService.getUserRoleIds(receiverId);
        // 身份优先级：学生<助教<教师<管理员
        int minLevel = 4; // 1:学生 2:助教 3:教师 4:管理员
        if (receiverRoles.contains("ROLE_STUDENT")) minLevel = Math.min(minLevel, 1);
        if (receiverRoles.contains("ROLE_ASSISTANT")) minLevel = Math.min(minLevel, 2);
        if (receiverRoles.contains("ROLE_TEACHER")) minLevel = Math.min(minLevel, 3);
        if (receiverRoles.contains("ROLE_ADMIN")) minLevel = Math.min(minLevel, 4);

        // 权限判断
        if (isAdmin) {
            // 管理员可以向任何人发消息
        } else if (isTeacher) {
            if (minLevel == 4) {
                throw new RuntimeException("教师不能向管理员发送消息");
            }
        } else if (isAssistant) {
            if (minLevel > 2) {
                throw new RuntimeException("助教只能向助教或学生发送消息");
            }
        } else if (isStudent) {
            if (minLevel > 2) {
                throw new RuntimeException("学生只能向教师、助教或学生发送消息");
            }
        } else {
            throw new RuntimeException("未知用户角色，无法发送消息");
        }

        // 发送者角色：优先使用请求体中的 senderRole（需确认为当前用户拥有该角色），否则回退为用户最高角色
        String requestedSenderRole = messageRequestDTO.getSenderRole();
        String senderRole = null;
        if (requestedSenderRole != null && !requestedSenderRole.isBlank()) {
            String tmp = requestedSenderRole.trim().toUpperCase();
            // 兼容无前缀形式（如 ADMIN/TEACHER/ASSISTANT/STUDENT）
            final String normalized = tmp.startsWith("ROLE_") ? tmp : ("ROLE_" + tmp);
            // 允许的角色常量
            List<String> allowed = java.util.List.of("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_ASSISTANT", "ROLE_STUDENT");
            if (allowed.contains(normalized)) {
                boolean owns = authorities.stream().anyMatch(a -> a.getAuthority().equals(normalized));
                if (owns) {
                    senderRole = normalized;
                }
            }
        }
        if (senderRole == null) {
            if (isAdmin) {
                senderRole = "ROLE_ADMIN";
            } else if (isTeacher) {
                senderRole = "ROLE_TEACHER";
            } else if (isAssistant) {
                senderRole = "ROLE_ASSISTANT";
            } else if (isStudent) {
                senderRole = "ROLE_STUDENT";
            } else {
                senderRole = "UNKNOWN";
            }
        }

        MessageDTO dto = MessageDTO.builder()
                .title(messageRequestDTO.getTitle())
                .content(messageRequestDTO.getContent())
                .receiver(messageRequestDTO.getReceiver())
                .sender(userDetails.getUsername())
                .senderRole(senderRole)
                .build();
        return Result.success(messageService.createMessage(dto));
    }

    @GetMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "获取消息", description = "根据ID获取消息详情")
    public Result<MessageDTO> getMessageById(@PathVariable String id) {
        return Result.success(messageService.getMessageById(id));
    }

    @GetMapping("/receiver")
    @io.swagger.v3.oas.annotations.Operation(summary = "获取当前用户接收的消息", description = "获取当前登录用户接收到的所有消息")
    public Result<List<MessageDTO>> getMessagesByReceiver(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new RuntimeException("未登录用户无法获取消息");
        }
        List<MessageDTO> messages = messageService.getMessagesByReceiver(userDetails.getUsername());
        return Result.success(messages);
    }

    @GetMapping("/sender/{sender}")
    @io.swagger.v3.oas.annotations.Operation(summary = "获取指定发送者发给当前用户的消息", description = "获取指定发送者发给当前登录用户的消息列表")
    public Result<List<MessageDTO>> getMessagesBySender(@PathVariable String sender, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new RuntimeException("未登录用户无法获取消息");
        }
        List<MessageDTO> messages = messageService.getMessagesBySenderAndReceiver(sender, userDetails.getUsername());
        return Result.success(messages);
    }

    @GetMapping("/senders")
    @io.swagger.v3.oas.annotations.Operation(summary = "获取所有给当前用户发送消息的发送者", description = "返回所有给当前登录用户发送消息的发送者信息，包括用户名、id和权限等级")
    public Result<List<SenderInfoDTO>> getSendersByReceiver(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new RuntimeException("未登录用户无法获取发送者列表");
        }
        List<SenderInfoDTO> senders = messageService.getSendersByReceiver(userDetails.getUsername());
        return Result.success(senders);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @io.swagger.v3.oas.annotations.Operation(summary = "获取所有消息", description = "仅管理员可获取所有消息")
    public Result<List<MessageDTO>> getAllMessages() {
        List<MessageDTO> messages = messageService.getAllMessages();
        return Result.success(messages);
    }

    @PutMapping("/{id}/read")
    @io.swagger.v3.oas.annotations.Operation(summary = "标记为已读", description = "将指定消息标记为已读")
    public Result<MessageDTO> markAsRead(@PathVariable String id) {
        return Result.success(messageService.markAsRead(id));
    }

    @DeleteMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "删除消息", description = "删除指定ID的消息")
    public Result<Void> deleteMessage(@PathVariable String id) {
        messageService.deleteMessage(id);
        return Result.success();
    }

    @GetMapping("/self/sent")
    @io.swagger.v3.oas.annotations.Operation(summary = "获取自己以当前权限等级发送的所有消息", description = "获取当前登录用户以当前登录权限等级发送的所有消息")
    public Result<List<MessageDTO>> getSelfSentMessages(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new RuntimeException("未登录用户无法获取消息");
        }
        String username = userDetails.getUsername();
        // 获取当前登录用户的最高权限等级
        var authorities = userDetails.getAuthorities();
        String senderRole;
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            senderRole = "ROLE_ADMIN";
        } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            senderRole = "ROLE_TEACHER";
        } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ASSISTANT"))) {
            senderRole = "ROLE_ASSISTANT";
        } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
            senderRole = "ROLE_STUDENT";
        } else {
            senderRole = "UNKNOWN";
        }
        List<MessageDTO> messages = messageService.getMessagesBySenderAndRole(username, senderRole);
        return Result.success(messages);
    }
}
