package org.linghu.mybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息通知DTO，用于返回消息通知信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private String id;
    private String title;
    private String content;
    private String sender;
    private String receiver;
    private String status; // 已读/未读
    private String createdAt;
    private String updatedAt;
    private String senderRole; // 发送者权限等级
}
