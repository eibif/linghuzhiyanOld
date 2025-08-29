package org.linghu.mybackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.constants.TaskType;
import org.linghu.mybackend.domain.*;
import org.linghu.mybackend.dto.*;
import org.linghu.mybackend.repository.*;
import org.linghu.mybackend.service.QuestionService;
import org.linghu.mybackend.util.MinioUtil;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * StudentExperimentServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class StudentExperimentServiceImplTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExperimentTaskRepository experimentTaskRepository;

    @Mock
    private ExperimentAssignmentRepository assignmentRepository;

    @Mock
    private ExperimentSubmissionRepository submissionRepository;

    @Mock
    private ExperimentEvaluationRepository evaluationRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private MinioUtil minioUtil;

    @InjectMocks
    private StudentExperimentServiceImpl studentExperimentService;

    private User testStudent;
    private Experiment testExperiment;
    private ExperimentTask testTask;
    private ExperimentAssignment testAssignment;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        testStudent = new User();
        testStudent.setId("user1");
        testStudent.setUsername("student1");
        testStudent.setEmail("student1@test.com");

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

        testAssignment = new ExperimentAssignment();
        testAssignment.setId("assignment1");
        testAssignment.setTaskId("task1");
        testAssignment.setUserId("user1");
        testAssignment.setAssignedAt(new java.util.Date());
    }

    @Nested
    class GetStudentExperimentsTests {

        @Test
        void getStudentExperiments_WithValidStudent_ShouldReturnExperiments() {
            // Given
            when(experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED))
                    .thenReturn(List.of(testExperiment));

            // When
            List<ExperimentDTO> result = studentExperimentService.getStudentExperiments("student1");

            // Then
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals("experiment1", result.get(0).getId());
            assertEquals("Test Experiment", result.get(0).getName());
            verify(experimentRepository).findByStatus(Experiment.ExperimentStatus.PUBLISHED);
        }

        @Test
        void getStudentExperiments_WithNoExperiments_ShouldReturnEmptyList() {
            // Given
            when(experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED))
                    .thenReturn(List.of());

            // When
            List<ExperimentDTO> result = studentExperimentService.getStudentExperiments("student1");

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(experimentRepository).findByStatus(Experiment.ExperimentStatus.PUBLISHED);
        }

        @Test
        void getStudentExperiments_WithMultipleExperiments_ShouldReturnAllExperiments() {
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

            when(experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED))
                    .thenReturn(List.of(testExperiment, experiment2));

            // When
            List<ExperimentDTO> result = studentExperimentService.getStudentExperiments("student1");

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(experimentRepository).findByStatus(Experiment.ExperimentStatus.PUBLISHED);
        }

        @Test
        void getStudentExperiments_WithRepositoryException_ShouldThrowException() {
            // Given
            when(experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                studentExperimentService.getStudentExperiments("student1");
            });
            assertEquals("Database error", exception.getMessage());
            verify(experimentRepository).findByStatus(Experiment.ExperimentStatus.PUBLISHED);
        }
    }

    @Nested
    class GetExperimentDetailsTests {

        @Test
        void getExperimentDetails_WithValidId_ShouldReturnExperiment() {
            // Given
            when(experimentRepository.findById("experiment1")).thenReturn(Optional.of(testExperiment));

            // When
            ExperimentDTO result = studentExperimentService.getExperimentDetails("experiment1");

            // Then
            assertNotNull(result);
            assertEquals("experiment1", result.getId());
            assertEquals("Test Experiment", result.getName());
            assertEquals("Test Description", result.getDescription());
            verify(experimentRepository).findById("experiment1");
        }

        @Test
        void getExperimentDetails_WithNonExistentId_ShouldThrowException() {
            // Given
            when(experimentRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                studentExperimentService.getExperimentDetails("nonexistent");
            });
            assertEquals("实验不存在", exception.getMessage());
            verify(experimentRepository).findById("nonexistent");
        }
    }

    @Nested
    class GetAssignedTasksTests {

        @Test
        void getAssignedTasks_WithValidStudent_ShouldReturnTasks() {
            // Given
            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.findByUserId("user1")).thenReturn(List.of(testAssignment));
            when(experimentTaskRepository.findById("task1")).thenReturn(Optional.of(testTask));

            // When
            List<ExperimentTaskDTO> result = studentExperimentService.getAssignedTasks("student1");

            // Then
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals("task1", result.get(0).getId());
            assertEquals("Test Task", result.get(0).getTitle());
            verify(userRepository).findByUsername("student1");
            verify(assignmentRepository).findByUserId("user1");
            verify(experimentTaskRepository).findById("task1");
        }

        @Test
        void getAssignedTasks_WithNonExistentStudent_ShouldThrowException() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                studentExperimentService.getAssignedTasks("nonexistent");
            });
            assertEquals("用户不存在", exception.getMessage());
            verify(userRepository).findByUsername("nonexistent");
            verify(assignmentRepository, never()).findByUserId(any());
        }

        @Test
        void getAssignedTasks_WithNoAssignments_ShouldReturnEmptyList() {
            // Given
            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.findByUserId("user1")).thenReturn(List.of());

            // When
            List<ExperimentTaskDTO> result = studentExperimentService.getAssignedTasks("student1");

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(userRepository).findByUsername("student1");
            verify(assignmentRepository).findByUserId("user1");
        }
    }

    @Nested
    class GetTaskByIdTests {

        @Test
        void getTaskById_WithValidTaskAndAuthorizedStudent_ShouldReturnTask() {
            // Given
            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("task1", "user1")).thenReturn(true);
            when(experimentTaskRepository.findById("task1")).thenReturn(Optional.of(testTask));

            // When
            ExperimentTaskDTO result = studentExperimentService.getTaskById("task1", "student1");

            // Then
            assertNotNull(result);
            assertEquals("task1", result.getId());
            assertEquals("Test Task", result.getTitle());
            verify(userRepository).findByUsername("student1");
            verify(assignmentRepository).existsByTaskIdAndUserId("task1", "user1");
            verify(experimentTaskRepository).findById("task1");
        }

        @Test
        void getTaskById_WithUnauthorizedStudent_ShouldThrowException() {
            // Given
            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("task1", "user1")).thenReturn(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                studentExperimentService.getTaskById("task1", "student1");
            });
            assertEquals("你没有权限访问该任务", exception.getMessage());
            verify(userRepository).findByUsername("student1");
            verify(assignmentRepository).existsByTaskIdAndUserId("task1", "user1");
            verify(experimentTaskRepository, never()).findById(any());
        }

        @Test
        void getTaskById_WithNonExistentStudent_ShouldThrowException() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                studentExperimentService.getTaskById("task1", "nonexistent");
            });
            assertEquals("用户不存在", exception.getMessage());
            verify(userRepository).findByUsername("nonexistent");
            verify(assignmentRepository, never()).existsByTaskIdAndUserId(any(), any());
        }

        @Test
        void getTaskById_WithNonExistentTask_ShouldThrowException() {
            // Given
            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("nonexistent", "user1")).thenReturn(true);
            when(experimentTaskRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                studentExperimentService.getTaskById("nonexistent", "student1");
            });
            assertEquals("任务不存在", exception.getMessage());
            verify(experimentTaskRepository).findById("nonexistent");
        }
    }

    @Nested
    class SubmitTaskTests {

        @Test
        void submitTask_WithValidData_ShouldSubmitSuccessfully() {
            // Given
            SubmissionRequestDTO submissionRequest = SubmissionRequestDTO.builder()
                    .taskId("task1")
                    .sourceCode("public class Test {}")
                    .build();

            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("task1", "user1")).thenReturn(true);
            when(experimentTaskRepository.findById("task1")).thenReturn(Optional.of(testTask));

            ExperimentSubmission savedSubmission = new ExperimentSubmission();
            savedSubmission.setId("submission1");
            savedSubmission.setTaskId("task1");
            savedSubmission.setUserId("user1");
            savedSubmission.setSourceCode("public class Test {}");
            savedSubmission.setSubmissionTime(LocalDateTime.now());

            when(submissionRepository.save(any(ExperimentSubmission.class))).thenReturn(savedSubmission);

            // When
            ExperimentSubmissionDTO result = studentExperimentService.submitTask(submissionRequest, "student1");

            // Then
            assertNotNull(result);
            assertEquals("submission1", result.getId());
            assertEquals("task1", result.getTaskId());
            assertEquals("user1", result.getUserId());
            verify(userRepository).findByUsername("student1");
            verify(assignmentRepository).existsByTaskIdAndUserId("task1", "user1");
            verify(experimentTaskRepository).findById("task1");
            verify(submissionRepository).save(any(ExperimentSubmission.class));
        }

        @Test
        void submitTask_WithUnauthorizedStudent_ShouldThrowException() {
            // Given
            SubmissionRequestDTO submissionRequest = SubmissionRequestDTO.builder()
                    .taskId("task1")
                    .sourceCode("public class Test {}")
                    .build();

            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("task1", "user1")).thenReturn(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                studentExperimentService.submitTask(submissionRequest, "student1");
            });
            assertEquals("你没有权限提交该任务", exception.getMessage());
            verify(submissionRepository, never()).save(any());
        }

        @Test
        void submitTask_WithNonExistentTask_ShouldThrowException() {
            // Given
            SubmissionRequestDTO submissionRequest = SubmissionRequestDTO.builder()
                    .taskId("nonexistent")
                    .sourceCode("public class Test {}")
                    .build();

            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("nonexistent", "user1")).thenReturn(true);
            when(experimentTaskRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                studentExperimentService.submitTask(submissionRequest, "student1");
            });
            assertEquals("任务不存在", exception.getMessage());
            verify(submissionRepository, never()).save(any());
        }
    }

    @Nested
    class GetTaskEvaluationResultTests {

        @Test
        void getTaskEvaluationResult_WithValidData_ShouldReturnResult() {
            // Given
            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("task1", "user1")).thenReturn(true);

            ExperimentEvaluation evaluation = new ExperimentEvaluation();
            evaluation.setId("eval1");
            evaluation.setTaskId("task1");
            evaluation.setUserId("user1");
            evaluation.setPassed(true);
            evaluation.setScore(85);
            evaluation.setEvaluatedAt(LocalDateTime.now());

            when(evaluationRepository.findTopByTaskIdAndUserIdOrderByEvaluatedAtDesc("task1", "user1"))
                    .thenReturn(Optional.of(evaluation));

            // When
            ExperimentEvaluationDTO result = studentExperimentService.getTaskEvaluationResult("task1", "student1");

            // Then
            assertNotNull(result);
            assertEquals("eval1", result.getId());
            assertEquals("task1", result.getTaskId());
            assertEquals("user1", result.getUserId());
            assertTrue(result.getPassed());
            assertEquals(85, result.getScore());
            verify(userRepository).findByUsername("student1");
            verify(assignmentRepository).existsByTaskIdAndUserId("task1", "user1");
            verify(evaluationRepository).findTopByTaskIdAndUserIdOrderByEvaluatedAtDesc("task1", "user1");
        }

        @Test
        void getTaskEvaluationResult_WithNoEvaluation_ShouldThrowException() {
            // Given
            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("task1", "user1")).thenReturn(true);
            when(evaluationRepository.findTopByTaskIdAndUserIdOrderByEvaluatedAtDesc("task1", "user1"))
                    .thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                studentExperimentService.getTaskEvaluationResult("task1", "student1");
            });
            assertEquals("未找到评测结果", exception.getMessage());
        }

        @Test
        void getTaskEvaluationResult_WithUnauthorizedStudent_ShouldThrowException() {
            // Given
            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("task1", "user1")).thenReturn(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                studentExperimentService.getTaskEvaluationResult("task1", "student1");
            });
            assertEquals("你没有权限查看该任务的评测结果", exception.getMessage());
            verify(evaluationRepository, never()).findTopByTaskIdAndUserIdOrderByEvaluatedAtDesc(any(), any());
        }
    }

    @Nested
    class GetTaskEvaluationHistoryTests {

        @Test
        void getTaskEvaluationHistory_WithValidData_ShouldReturnHistory() {
            // Given
            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("task1", "user1")).thenReturn(true);

            ExperimentEvaluation evaluation1 = new ExperimentEvaluation();
            evaluation1.setId("eval1");
            evaluation1.setTaskId("task1");
            evaluation1.setUserId("user1");
            evaluation1.setPassed(false);
            evaluation1.setScore(60);
            evaluation1.setEvaluatedAt(LocalDateTime.now().minusDays(1));

            ExperimentEvaluation evaluation2 = new ExperimentEvaluation();
            evaluation2.setId("eval2");
            evaluation2.setTaskId("task1");
            evaluation2.setUserId("user1");
            evaluation2.setPassed(true);
            evaluation2.setScore(85);
            evaluation2.setEvaluatedAt(LocalDateTime.now());

            when(evaluationRepository.findByTaskIdAndUserIdOrderByEvaluatedAtDesc("task1", "user1"))
                    .thenReturn(List.of(evaluation2, evaluation1));

            // When
            List<ExperimentEvaluationDTO> result = studentExperimentService.getTaskEvaluationHistory("task1", "student1");

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("eval2", result.get(0).getId());
            assertEquals("eval1", result.get(1).getId());
            verify(userRepository).findByUsername("student1");
            verify(assignmentRepository).existsByTaskIdAndUserId("task1", "user1");
            verify(evaluationRepository).findByTaskIdAndUserIdOrderByEvaluatedAtDesc("task1", "user1");
        }

        @Test
        void getTaskEvaluationHistory_WithNoHistory_ShouldReturnEmptyList() {
            // Given
            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("task1", "user1")).thenReturn(true);
            when(evaluationRepository.findByTaskIdAndUserIdOrderByEvaluatedAtDesc("task1", "user1"))
                    .thenReturn(List.of());

            // When
            List<ExperimentEvaluationDTO> result = studentExperimentService.getTaskEvaluationHistory("task1", "student1");

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(evaluationRepository).findByTaskIdAndUserIdOrderByEvaluatedAtDesc("task1", "user1");
        }

        @Test
        void getTaskEvaluationHistory_WithUnauthorizedStudent_ShouldThrowException() {
            // Given
            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("task1", "user1")).thenReturn(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                studentExperimentService.getTaskEvaluationHistory("task1", "student1");
            });
            assertEquals("你没有权限查看该任务的评测历史", exception.getMessage());
            verify(evaluationRepository, never()).findByTaskIdAndUserIdOrderByEvaluatedAtDesc(any(), any());
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void getStudentExperiments_WithNullUsername_ShouldHandleGracefully() {
            // Given
            when(experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED))
                    .thenReturn(List.of(testExperiment));

            // When
            List<ExperimentDTO> result = studentExperimentService.getStudentExperiments(null);

            // Then
            assertNotNull(result);
            assertFalse(result.isEmpty());
            verify(experimentRepository).findByStatus(Experiment.ExperimentStatus.PUBLISHED);
        }

        @Test
        void submitTask_WithNullSourceCode_ShouldAcceptSubmission() {
            // Given
            SubmissionRequestDTO submissionRequest = SubmissionRequestDTO.builder()
                    .taskId("task1")
                    .sourceCode(null)
                    .build();

            when(userRepository.findByUsername("student1")).thenReturn(Optional.of(testStudent));
            when(assignmentRepository.existsByTaskIdAndUserId("task1", "user1")).thenReturn(true);
            when(experimentTaskRepository.findById("task1")).thenReturn(Optional.of(testTask));

            ExperimentSubmission savedSubmission = new ExperimentSubmission();
            savedSubmission.setId("submission1");
            savedSubmission.setTaskId("task1");
            savedSubmission.setUserId("user1");
            savedSubmission.setSourceCode(null);
            savedSubmission.setSubmissionTime(LocalDateTime.now());

            when(submissionRepository.save(any(ExperimentSubmission.class))).thenReturn(savedSubmission);

            // When
            ExperimentSubmissionDTO result = studentExperimentService.submitTask(submissionRequest, "student1");

            // Then
            assertNotNull(result);
            assertEquals("submission1", result.getId());
            verify(submissionRepository).save(any(ExperimentSubmission.class));
        }

        @Test
        void getExperimentDetails_WithNullId_ShouldThrowException() {
            // When & Then
            assertThrows(RuntimeException.class, () -> {
                studentExperimentService.getExperimentDetails(null);
            });
        }
    }
}
