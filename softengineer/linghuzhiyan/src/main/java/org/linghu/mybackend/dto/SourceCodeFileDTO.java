package org.linghu.mybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 源代码文件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceCodeFileDTO {
    private String fileName; // 文件名
    private String content;  // 文件内容
}
