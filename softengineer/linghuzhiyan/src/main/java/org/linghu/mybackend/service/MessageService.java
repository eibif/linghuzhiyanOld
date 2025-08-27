package org.linghu.mybackend.service;

import java.util.List;

import org.linghu.mybackend.dto.MessageDTO;
import org.linghu.mybackend.dto.SenderInfoDTO;

/**
 * 消息通知服务接口
 */
public interface MessageService {
    MessageDTO createMessage(MessageDTO messageDTO);
    MessageDTO getMessageById(String id);
    List<MessageDTO> getMessagesByReceiver(String receiver);
    /**
     * 获取指定发送者发给指定接收者的消息
     */
    List<MessageDTO> getMessagesBySenderAndReceiver(String sender, String receiver);

    /**
     * 获取指定发送者以指定权限等级发送的所有消息
     */
    List<MessageDTO> getMessagesBySenderAndRole(String sender, String senderRole);

    /**
     * 获取所有消息（仅管理员可用）
     */
    List<MessageDTO> getAllMessages();
    MessageDTO markAsRead(String id);
    void deleteMessage(String id);

    /**
     * 获取给指定接收者发送消息的所有发送者信息（用户名、id、权限等级）
     */
    List<SenderInfoDTO> getSendersByReceiver(String receiverUsername);
}
