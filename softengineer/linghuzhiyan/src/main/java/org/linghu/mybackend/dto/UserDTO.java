package org.linghu.mybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

/**
 * 用户数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private String id;
    private String username;
    private String email;
    private String avatar;     // 存储在MinIO中的头像路径
    private String avatarUrl;  // 头像的访问URL，前端可直接使用
    private Object profile; 
    private Set<String> roles; 
    private String createdAt;
    private String updatedAt;
    private Boolean isDeleted;
}
