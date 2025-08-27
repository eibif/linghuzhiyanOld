package org.linghu.mybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 实验提交DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentSubmissionDTO {
    private String id;
    private String experiment_id;
    private String task_id;
    private String user_id;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Double score;
    private LocalDateTime submission_time;
    private Object user_answer; 
}
