package org.linghu.mybackend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * 实验任务分配领域模型，对应数据库中的experiment_assignment表
 */
@Entity
@Table(name = "experiment_assignment", uniqueConstraints = {
    @UniqueConstraint(name = "uk_experiment_user", columnNames = {"task_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentAssignment {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date assignedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ExperimentTask task;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        assignedAt = new Date();
    }
}
