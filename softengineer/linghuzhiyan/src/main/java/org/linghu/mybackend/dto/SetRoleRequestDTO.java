package org.linghu.mybackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设置用户角色请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetRoleRequestDTO {
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    @NotBlank(message = "角色不能为空")
    private String roleId;
}
