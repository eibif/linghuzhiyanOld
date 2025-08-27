package org.linghu.mybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 实验评测结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentEvaluationDTO {
    // 评测信息
    private String id;
    private String submissionId;
    private BigDecimal score;
    private String errorMessage;
    private String additionalInfo;
    
    // 提交相关的额外信息
    private String taskId;
    private String userId;  // 学生ID
    private LocalDateTime submitTime;
    private String userAnswer; // 提交的答案内容
    private String status;
}
