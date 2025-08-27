package org.linghu.mybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 资源创建/更新请求数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequestDTO {
    @NotNull(message = "实验ID不能为空")
    private String experimentId;
    
    private String taskId;  // 任务ID，可选
    
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;    // "DOCUMENT", "IMAGE", "VIDEO", "CODE", "OTHER"
    
    private String description;
    
    // 资源上传类型：学习资料("resource")和源代码/评测脚本("experiment")
    @NotBlank(message = "上传类型不能为空")
    private String uploadType;  // "resource" 或 "experiment"
    
    // 是否自动解压压缩包（仅对experiment类型的压缩文件有效）
    @Builder.Default
    private Boolean autoExtract = true;
}
