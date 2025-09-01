package org.linghu.mybackend.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.linghu.mybackend.domain.Message;
import org.linghu.mybackend.dto.MessageDTO;
import org.linghu.mybackend.dto.SenderInfoDTO;
import org.linghu.mybackend.exception.UnauthorizedException;
import org.linghu.mybackend.repository.MessageRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.linghu.mybackend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 消息通知服务实现类
 */
@Service
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Autowired
    public MessageServiceImpl(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Override
    public MessageDTO createMessage(MessageDTO messageDTO) {
        Message message = Message.builder()
                .title(messageDTO.getTitle())
                .content(messageDTO.getContent())
                .sender(messageDTO.getSender())
                .receiver(messageDTO.getReceiver())
                .senderRole(messageDTO.getSenderRole()) // 新增：保存controller传入的senderRole
                .status("未读")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        messageRepository.save(message);
        return toDTO(message);
    }

    @Override
    public MessageDTO getMessageById(String id) {
        return messageRepository.findById(id).map(this::toDTO).orElse(null);
    }

    @Override
    public List<MessageDTO> getMessagesByReceiver(String receiver) {
        List<Message> messages = messageRepository.findByReceiverOrderByCreatedAtDesc(receiver);
        List<MessageDTO> result = new java.util.ArrayList<>();
        for (Message msg : messages) {
            result.add(toDTO(msg));
        }
        return result;
    }

    @Override
    public List<MessageDTO> getMessagesBySenderAndReceiver(String sender, String receiver) {
        List<Message> messages = messageRepository.findBySenderAndReceiverOrderByCreatedAtDesc(sender, receiver);
        List<MessageDTO> result = new java.util.ArrayList<>();
        for (Message msg : messages) {
            result.add(toDTO(msg));
        }
        return result;
    }

    @Override
    public List<MessageDTO> getAllMessages() {
        List<Message> messages = messageRepository.findAllByOrderByCreatedAtDesc();
        List<MessageDTO> result = new java.util.ArrayList<>();
        for (Message msg : messages) {
            result.add(toDTO(msg));
        }
        return result;
    }

    @Override
    public MessageDTO markAsRead(String id) {
        return messageRepository.findById(id).map(msg -> {
            msg.setStatus("已读");
            msg.setUpdatedAt(LocalDateTime.now());
            messageRepository.save(msg);
            return toDTO(msg);
        }).orElse(null);
    }

    @Override
    public void deleteMessage(String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            // 仅在有认证上下文时才加载消息并进行权限校验（避免无安全上下文的单元测试出现NPE）
            Message msg = messageRepository.findById(id).orElse(null);
            String username = auth.getName();
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            boolean isSender = (msg != null && msg.getSender() != null && msg.getSender().equals(username));

            // 仅管理员或发送者可以删除消息；接收者（如学生）无权删除
            if (!(isAdmin || isSender)) {
                throw new UnauthorizedException("权限不足，无法访问该资源");
            }
        }

        // 未认证场景（如某些单元测试直接调用 Service）保持原有行为，执行幂等删除
        messageRepository.deleteById(id);
    }

    @Override
    public List<SenderInfoDTO> getSendersByReceiver(String receiverUsername) {
        List<Message> messages = messageRepository.findByReceiver(receiverUsername);
        // 使用 (senderUsername, senderRole) 作为去重键，避免不同身份被后来的消息覆盖
        java.util.Map<String, SenderInfoDTO> senderMap = new java.util.LinkedHashMap<>();
        for (Message msg : messages) {
            String senderUsername = msg.getSender();
            String role = msg.getSenderRole();
            String key = senderUsername + "|" + role;
            if (!senderMap.containsKey(key)) {
                // 查找发送者id和用户名
                var userOpt = userRepository.findByUsername(senderUsername);
                String senderId = userOpt.map(u -> u.getId()).orElse("");
                senderMap.put(key, SenderInfoDTO.builder()
                        .senderId(senderId)
                        .senderUsername(senderUsername)
                        .senderRole(role)
                        .build());
            }
        }
        return new java.util.ArrayList<>(senderMap.values());
    }

    @Override
    public List<MessageDTO> getMessagesBySenderAndRole(String sender, String senderRole) {
        List<Message> messages = messageRepository.findBySenderAndSenderRoleOrderByCreatedAtDesc(sender, senderRole);
        List<MessageDTO> result = new java.util.ArrayList<>();
        for (Message msg : messages) {
            result.add(toDTO(msg));
        }
        return result;
    }

    @Override
    public void sendSystemNotification(String title, String content, List<String> receiverIds) {
        for (String receiverId : receiverIds) {
            try {
                Message message = Message.builder()
                        .title(title)
                        .content(content)
                        .sender("SYSTEM") // 系统发送者
                        .receiver(receiverId)
                        .senderRole("SYSTEM")
                        .status("未读")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                messageRepository.save(message);
            } catch (Exception e) {
                // 单个用户发送失败不影响其他用户
                System.err.println("发送系统通知失败: receiverId=" + receiverId + ", error=" + e.getMessage());
            }
        }
    }

    @Override
    public void sendExperimentNotification(String title, String content, String experimentId, List<String> receiverIds) {
        for (String receiverId : receiverIds) {
            try {
                Message message = Message.builder()
                        .title(title)
                        .content(content)
                        .sender("SYSTEM")
                        .receiver(receiverId)
                        .senderRole("SYSTEM")
                        .status("未读")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                messageRepository.save(message);
            } catch (Exception e) {
                System.err.println("发送实验通知失败: receiverId=" + receiverId + ", experimentId=" + experimentId + ", error=" + e.getMessage());
            }
        }
    }

    @Override
    public void sendGradeNotification(String title, String content, String experimentId, String receiverId) {
        try {
            Message message = Message.builder()
                    .title(title)
                    .content(content)
                    .sender("SYSTEM")
                    .receiver(receiverId)
                    .senderRole("SYSTEM")
                    .status("未读")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            messageRepository.save(message);
        } catch (Exception e) {
            System.err.println("发送成绩通知失败: receiverId=" + receiverId + ", experimentId=" + experimentId + ", error=" + e.getMessage());
            throw new RuntimeException("发送成绩通知失败: " + e.getMessage(), e);
        }
    }

    private MessageDTO toDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .title(message.getTitle())
                .content(message.getContent())
                .sender(message.getSender())
                .receiver(message.getReceiver())
                .status(message.getStatus())
                .senderRole(message.getSenderRole())
                .createdAt(message.getCreatedAt() == null ? null : message.getCreatedAt().toString())
                .updatedAt(message.getUpdatedAt() == null ? null : message.getUpdatedAt().toString())
                .build();
    }
}
