package org.linghu.mybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.linghu.mybackend.constants.ExperimentConstants;
import org.linghu.mybackend.domain.Experiment;

import java.sql.Date;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentRequestDTO {
    private String id;
    private String name;
    private String description;
    private Experiment.ExperimentStatus status; // "DRAFT", "PUBLISHED"
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
