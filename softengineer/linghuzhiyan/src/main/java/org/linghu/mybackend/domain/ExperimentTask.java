package org.linghu.mybackend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.linghu.mybackend.constants.TaskType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实验任务领域模型，对应数据库中的experiment_task表
 */
@Entity
@Table(name = "experiment_task")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentTask {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "experiment_id", nullable = false, length = 36)
    private String experimentId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "question_ids", columnDefinition = "json")
    private String questionIds;

    @Transient
    private List<Question> questions;
    
    @Builder.Default
    @Column(nullable = false, columnDefinition = "tinyint(1) default 1")
    private Boolean required = true;
    
    @Builder.Default
    @Column(name = "order_num", nullable = false, columnDefinition = "int default 0")
    private Integer orderNum = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType = TaskType.OTHER;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Experiment experiment;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
