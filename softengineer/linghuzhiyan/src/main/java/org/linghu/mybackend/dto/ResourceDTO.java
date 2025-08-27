package org.linghu.mybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 资源数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDTO {
    private String id;
    private String experimentId;
    private String resourceType;    // "DOCUMENT", "IMAGE", "VIDEO", "CODE", "OTHER"
    private String resourcePath;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String description;
    private LocalDateTime uploadTime;
    
}
