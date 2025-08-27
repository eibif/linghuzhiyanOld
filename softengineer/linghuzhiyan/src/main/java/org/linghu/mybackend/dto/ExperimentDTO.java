package org.linghu.mybackend.dto;

import java.time.LocalDateTime;

import org.linghu.mybackend.domain.Experiment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentDTO {
    private String id;
    private String creator_Id;
    private String name;
    private String description;
    private Experiment.ExperimentStatus status; // "DRAFT", "PUBLISHED"
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
