package org.linghu.mybackend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 实验评测领域模型，对应数据库中的experiment_evaluation表
 */
@Entity
@Table(name = "experiment_evaluation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentEvaluation {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(name = "submission_id", nullable = false, length = 36)
    private String submissionId;
      @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", insertable = false, updatable = false)
    @ToString.Exclude
    private ExperimentSubmission submission;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private ExperimentTask task;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;
    
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
    
    @Column(name = "additional_info", columnDefinition = "text")
    private String additionalInfo;
}
