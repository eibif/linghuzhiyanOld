package org.linghu.mybackend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
/**
 * 实验任务提交领域模型，对应数据库中的experiment_submission表
 */
@Entity
@Table(name = "experiment_submission")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentSubmission {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "submit_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime submitTime;

    @Column(name = "user_answer", columnDefinition = "json")
    private String userAnswer;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal score;


    @Column(name = "grader_id", length = 36)
    private String graderId;

    @Column(name = "graded_time")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime gradedTime;

    @Column(name = "time_spent")
    private Integer timeSpent;    
    
    // 实验任务关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private ExperimentTask experimentTask;
    
    // 提交用户关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private User user;
    
    // 评分教师关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grader_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private User grader;
    
    // 创建和更新时间戳
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ExperimentEvaluation> evaluations;
    
    @PrePersist
    protected void onCreate() {
        if (submitTime == null) {
            submitTime = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
