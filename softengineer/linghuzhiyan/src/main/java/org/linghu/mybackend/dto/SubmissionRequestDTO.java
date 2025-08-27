package org.linghu.mybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 实验任务提交请求DTO
 * 用于学生提交实验任务时传递数据
 * 统一处理常规答案和代码提交
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRequestDTO {
    private String taskId;         // 任务ID
    private String experimentId;   // 实验ID
    private Object userAnswer;     // 用户答案（用于非代码类型的提交）
    private List<SourceCodeFileDTO> files; // 源代码文件列表（代码提交专用）
}
