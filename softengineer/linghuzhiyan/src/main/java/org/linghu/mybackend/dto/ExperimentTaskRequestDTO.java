package org.linghu.mybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.linghu.mybackend.constants.TaskType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExperimentTaskRequestDTO {
    private String title; // 任务名称
    private String description; // 任务描述
    private TaskType taskType; // 任务类型
    private Object question; // 任务问题
    private Boolean required; // 是否允许迟交
}
