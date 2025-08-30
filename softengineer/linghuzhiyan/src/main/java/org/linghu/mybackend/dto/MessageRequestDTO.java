package org.linghu.mybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建消息通知DTO，仅用于创建消息时的参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequestDTO {
    private String title;
    private String content;
    private String receiver;
    /**
     * 可选：由前端显式指定的发送者角色，例如：ROLE_ADMIN/ROLE_TEACHER/ROLE_ASSISTANT/ROLE_STUDENT。
     * 后端会校验当前登录用户是否实际拥有该角色；若不合法则忽略并按当前用户最高角色记录。
     */
    private String senderRole;
}
