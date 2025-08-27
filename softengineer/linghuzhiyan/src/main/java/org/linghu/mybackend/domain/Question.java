package org.linghu.mybackend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.checkerframework.checker.units.qual.C;

/**
 * 题目领域模型，对应数据库中的question表
 */
@Entity
@Table(name = "question")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "score", nullable = false)
    private BigDecimal score;

    @Column(columnDefinition = "json")
    private String options;

    @Column(columnDefinition = "json")
    private String answer;

    @Column(columnDefinition = "text")
    private String explanation;

    private String tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    /**
     * 题目类型枚举
     */
    public enum QuestionType {
        SINGLE_CHOICE, // 单选题
        MULTIPLE_CHOICE, // 多选题
        FILL_BLANK, // 填空题
        QA // 问答题
    }
}
