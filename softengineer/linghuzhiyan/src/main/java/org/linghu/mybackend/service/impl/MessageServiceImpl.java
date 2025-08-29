package org.linghu.mybackend.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.linghu.mybackend.domain.Message;
import org.linghu.mybackend.dto.MessageDTO;
import org.linghu.mybackend.dto.SenderInfoDTO;
import org.linghu.mybackend.repository.MessageRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.linghu.mybackend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
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
        messageRepository.deleteById(id);
    }

    @Override
    public List<SenderInfoDTO> getSendersByReceiver(String receiverUsername) {
        List<Message> messages = messageRepository.findByReceiver(receiverUsername);
        java.util.Map<String, SenderInfoDTO> senderMap = new java.util.HashMap<>();
        for (Message msg : messages) {
            // 查找发送者id和用户名
            var userOpt = userRepository.findByUsername(msg.getSender());
            String senderId = userOpt.map(u -> u.getId()).orElse("");
            senderMap.put(msg.getSender(), SenderInfoDTO.builder()
                    .senderId(senderId)
                    .senderUsername(msg.getSender())
                    .senderRole(msg.getSenderRole())
                    .build());
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
