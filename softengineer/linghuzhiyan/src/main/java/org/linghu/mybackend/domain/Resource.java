package org.linghu.mybackend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资源领域模型，对应数据库中的resource表
 */
@Entity
@Table(name = "resource")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "experiment_id", length = 36)
    private String experimentId;    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, columnDefinition = "enum('DOCUMENT', 'IMAGE', 'VIDEO', 'CODE', 'OTHER', 'SUBMISSION', 'PRESENTATION', 'SPREADSHEET', 'AUDIO', 'ARCHIVE')")
    private ResourceType resourceType;

    @Column(name = "resource_path", nullable = false, length = 255)
    private String resourcePath;

    @Column(name = "file_name", length = 100)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "upload_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime uploadTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Experiment experiment;

    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
    }    // 资源类型枚举
    public enum ResourceType {
        DOCUMENT, IMAGE, VIDEO, CODE, OTHER, SUBMISSION, PRESENTATION, SPREADSHEET, AUDIO, ARCHIVE
    }
}
