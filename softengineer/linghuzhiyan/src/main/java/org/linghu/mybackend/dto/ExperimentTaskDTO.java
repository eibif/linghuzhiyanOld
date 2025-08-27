package org.linghu.mybackend.dto;

import org.linghu.mybackend.constants.TaskType;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentTaskDTO {
    private String id; // 任务ID
    private String experimentId; // 实验ID
    private String title; // 任务名称
    private String description; // 任务描述
    private int orderNum; // 任务顺序
    private TaskType taskType; // 任务类型
    private Object question; // 任务问题
    private Object answers; // 任务答案
    private Boolean required; // 是否允许迟交
    private List<SourceCodeFileDTO> files; // 源代码文件列表（仅当taskType为CODE时有效）

    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
