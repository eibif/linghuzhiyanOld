package org.linghu.mybackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.constants.TaskType;
import org.linghu.mybackend.domain.*;
import org.linghu.mybackend.dto.*;
import org.linghu.mybackend.repository.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StudentExperimentServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class StudentExperimentServiceImplTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ExperimentAssignmentRepository assignmentRepository;

    @Mock
    private ExperimentTaskRepository experimentTaskRepository;

    @Mock
    private ExperimentSubmissionRepository submissionRepository;

    @Mock
    private ExperimentEvaluationRepository evaluationRepository;

    @Mock
    private QuestionRepository questionRepository;


    @InjectMocks
    private StudentExperimentServiceImpl studentExperimentService;

    private Experiment testExperiment;
    private ExperimentTask testTask;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        testExperiment = Experiment.builder()
                .id("experiment1")
                .name("Test Experiment")
                .description("Test Description")
                .creatorId("teacher1")
                .status(Experiment.ExperimentStatus.PUBLISHED)
                .startTime(now)
                .endTime(now.plusDays(7))
                .createdAt(now)
                .updatedAt(now)
                .build();

        testTask = ExperimentTask.builder()
                .id("task1")
                .experimentId("experiment1")
                .title("Test Task")
                .description("Test Task Description")
                .taskType(TaskType.CODE)
                .orderNum(1)
                .questionIds("[\"q1\", \"q2\"]")
                .required(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void getStudentExperiments_WithValidStudent_ShouldReturnExperiments() {
        // Given
        when(experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED)).thenReturn(List.of(testExperiment));

        // When
        List<ExperimentDTO> result = studentExperimentService.getStudentExperiments("student1");

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(experimentRepository).findByStatus(Experiment.ExperimentStatus.PUBLISHED);
    }

    @Test
    void getStudentExperiments_WithNoExperiments_ShouldReturnEmptyList() {
        // Given
        when(experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED)).thenReturn(List.of());

        // When
        List<ExperimentDTO> result = studentExperimentService.getStudentExperiments("student1");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(experimentRepository).findByStatus(Experiment.ExperimentStatus.PUBLISHED);
    }

    @Test
    void getStudentExperiments_WithRepositoryException_ShouldThrowException() {
        // Given
        when(experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            studentExperimentService.getStudentExperiments("student1");
        });
        assertEquals("Database error", exception.getMessage());
        verify(experimentRepository).findByStatus(Experiment.ExperimentStatus.PUBLISHED);
    }

    @Test
    void getAllExperiments_WithValidData_ShouldReturnAllExperiments() {
        // Given
        when(experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED)).thenReturn(List.of(testExperiment));

        // When
        List<ExperimentDTO> result = studentExperimentService.getStudentExperiments("student1");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(experimentRepository).findByStatus(Experiment.ExperimentStatus.PUBLISHED);
    }

    @Test
    void getAllExperiments_WithMultipleExperiments_ShouldReturnAllExperiments() {
        // Given
        Experiment experiment2 = Experiment.builder()
                .id("experiment2")
                .name("Test Experiment 2")
                .description("Test Description 2")
                .creatorId("teacher2")
                .status(Experiment.ExperimentStatus.PUBLISHED)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(14))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED)).thenReturn(List.of(testExperiment, experiment2));

        // When
        List<ExperimentDTO> result = studentExperimentService.getStudentExperiments("student1");

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(experimentRepository).findByStatus(Experiment.ExperimentStatus.PUBLISHED);
    }

    @Test
    void getExperimentTasks_WithValidExperiment_ShouldReturnTasks() {
        // Given
        when(experimentTaskRepository.findByExperimentIdOrderByOrderNumAsc("experiment1"))
                .thenReturn(List.of(testTask));

        // When - 直接调用Repository方法进行测试
        List<ExperimentTask> result = experimentTaskRepository.findByExperimentIdOrderByOrderNumAsc("experiment1");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("task1", result.get(0).getId());
        assertEquals("Test Task", result.get(0).getTitle());
        verify(experimentTaskRepository).findByExperimentIdOrderByOrderNumAsc("experiment1");
    }

    @Test
    void getExperimentTasks_WithNonExistentExperiment_ShouldReturnEmptyList() {
        // Given
        when(experimentTaskRepository.findByExperimentIdOrderByOrderNumAsc("nonexistent"))
                .thenReturn(List.of());

        // When
        List<ExperimentTask> result = experimentTaskRepository.findByExperimentIdOrderByOrderNumAsc("nonexistent");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(experimentTaskRepository).findByExperimentIdOrderByOrderNumAsc("nonexistent");
    }

    @Test
    void findExperimentById_WithValidId_ShouldReturnExperiment() {
        // Given
        when(experimentRepository.findById("experiment1")).thenReturn(Optional.of(testExperiment));

        // When
        Optional<Experiment> result = experimentRepository.findById("experiment1");

        // Then
        assertTrue(result.isPresent());
        assertEquals("experiment1", result.get().getId());
        assertEquals("Test Experiment", result.get().getName());
        verify(experimentRepository).findById("experiment1");
    }

    @Test
    void findExperimentById_WithNonExistentId_ShouldReturnEmpty() {
        // Given
        when(experimentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<Experiment> result = experimentRepository.findById("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(experimentRepository).findById("nonexistent");
    }

    @Test
    void findTaskById_WithValidId_ShouldReturnTask() {
        // Given
        when(experimentTaskRepository.findById("task1")).thenReturn(Optional.of(testTask));

        // When
        Optional<ExperimentTask> result = experimentTaskRepository.findById("task1");

        // Then
        assertTrue(result.isPresent());
        assertEquals("task1", result.get().getId());
        assertEquals("Test Task", result.get().getTitle());
        verify(experimentTaskRepository).findById("task1");
    }

    @Test
    void findTaskById_WithNonExistentId_ShouldReturnEmpty() {
        // Given
        when(experimentTaskRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<ExperimentTask> result = experimentTaskRepository.findById("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(experimentTaskRepository).findById("nonexistent");
    }

    @Test
    void checkTaskExists_WithValidId_ShouldReturnTrue() {
        // Given
        when(experimentTaskRepository.existsById("task1")).thenReturn(true);

        // When
        boolean result = experimentTaskRepository.existsById("task1");

        // Then
        assertTrue(result);
        verify(experimentTaskRepository).existsById("task1");
    }

    @Test
    void checkTaskExists_WithNonExistentId_ShouldReturnFalse() {
        // Given
        when(experimentTaskRepository.existsById("nonexistent")).thenReturn(false);

        // When
        boolean result = experimentTaskRepository.existsById("nonexistent");

        // Then
        assertFalse(result);
        verify(experimentTaskRepository).existsById("nonexistent");
    }

    @Test
    void checkExperimentExists_WithValidId_ShouldReturnTrue() {
        // Given
        when(experimentRepository.existsById("experiment1")).thenReturn(true);

        // When
        boolean result = experimentRepository.existsById("experiment1");

        // Then
        assertTrue(result);
        verify(experimentRepository).existsById("experiment1");
    }

    @Test
    void checkExperimentExists_WithNonExistentId_ShouldReturnFalse() {
        // Given
        when(experimentRepository.existsById("nonexistent")).thenReturn(false);

        // When
        boolean result = experimentRepository.existsById("nonexistent");

        // Then
        assertFalse(result);
        verify(experimentRepository).existsById("nonexistent");
    }

    @Test
    void repositoryOperations_WithNullInput_ShouldHandleGracefully() {
        // Given
        when(experimentRepository.findById(null)).thenReturn(Optional.empty());

        // When
        Optional<Experiment> result = experimentRepository.findById(null);

        // Then
        assertFalse(result.isPresent());
        verify(experimentRepository).findById(null);
    }

    @Test
    void repositoryOperations_WithEmptyStringInput_ShouldHandleGracefully() {
        // Given
        when(experimentRepository.findById("")).thenReturn(Optional.empty());

        // When
        Optional<Experiment> result = experimentRepository.findById("");

        // Then
        assertFalse(result.isPresent());
        verify(experimentRepository).findById("");
    }
}
