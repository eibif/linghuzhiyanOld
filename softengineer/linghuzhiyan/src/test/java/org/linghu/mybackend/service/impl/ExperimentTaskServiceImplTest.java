package org.linghu.mybackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.constants.TaskType;
import org.linghu.mybackend.domain.ExperimentTask;
import org.linghu.mybackend.dto.ExperimentTaskDTO;
import org.linghu.mybackend.dto.ExperimentTaskRequestDTO;
import org.linghu.mybackend.repository.ExperimentRepository;
import org.linghu.mybackend.repository.ExperimentTaskRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExperimentTaskServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ExperimentTaskServiceImplTest {

    @Mock
    private ExperimentTaskRepository experimentTaskRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    @InjectMocks
    private ExperimentTaskServiceImpl experimentTaskService;

    private ExperimentTask testTask;
    private ExperimentTaskRequestDTO testTaskRequest;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        testTask = ExperimentTask.builder()
                .id("task1")
                .experimentId("experiment1")
                .title("Test Task")
                .description("Test Description")
                .taskType(TaskType.CODE)
                .orderNum(1)
                .questionIds("[\"q1\", \"q2\"]")
                .required(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        testTaskRequest = ExperimentTaskRequestDTO.builder()
                .title("Test Task")
                .description("Test Description")
                .taskType(TaskType.CODE)
                .required(true)
                .question(List.of("q1", "q2"))
                .build();
    }

    @Nested
    class CreateTaskTests {
        
        @Test
        void createTask_WithValidData_ShouldCreateSuccessfully() {
            // Given
            when(experimentRepository.existsById("experiment1")).thenReturn(true);
            when(experimentTaskRepository.findMaxOrderNumByExperimentId("experiment1")).thenReturn(0);
            when(experimentTaskRepository.save(any(ExperimentTask.class))).thenReturn(testTask);

            // When
            ExperimentTaskDTO result = experimentTaskService.createTask("experiment1", testTaskRequest);

            // Then
            assertNotNull(result);
            assertEquals("task1", result.getId());
            assertEquals("Test Task", result.getTitle());
            assertEquals("Test Description", result.getDescription());
            assertEquals(TaskType.CODE, result.getTaskType());
            assertTrue(result.getRequired());
            assertEquals(1, result.getOrderNum());
            verify(experimentTaskRepository).save(any(ExperimentTask.class));
        }

        @Test
        void createTask_WithNonExistingExperiment_ShouldThrowException() {
            // Given
            when(experimentRepository.existsById("nonexistent")).thenReturn(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                experimentTaskService.createTask("nonexistent", testTaskRequest);
            });
            assertEquals("实验不存在", exception.getMessage());
            verify(experimentTaskRepository, never()).save(any(ExperimentTask.class));
        }

        @Test
        void createTask_WithNullQuestion_ShouldCreateWithNullQuestionIds() {
            // Given
            ExperimentTaskRequestDTO requestWithoutQuestion = ExperimentTaskRequestDTO.builder()
                    .title("Test Task")
                    .description("Test Description")
                    .taskType(TaskType.CODE)
                    .required(true)
                    .question(null)
                    .build();

            when(experimentRepository.existsById("experiment1")).thenReturn(true);
            when(experimentTaskRepository.findMaxOrderNumByExperimentId("experiment1")).thenReturn(0);
            when(experimentTaskRepository.save(any(ExperimentTask.class))).thenReturn(testTask);

            // When
            ExperimentTaskDTO result = experimentTaskService.createTask("experiment1", requestWithoutQuestion);

            // Then
            assertNotNull(result);
            verify(experimentTaskRepository).save(any(ExperimentTask.class));
        }

        @Test
        void createTask_WithExistingTasks_ShouldSetCorrectOrderNumber() {
            // Given
            when(experimentRepository.existsById("experiment1")).thenReturn(true);
            when(experimentTaskRepository.findMaxOrderNumByExperimentId("experiment1")).thenReturn(3);
            
            ExperimentTask newTask = ExperimentTask.builder()
                    .id("task2")
                    .experimentId("experiment1")
                    .title("Test Task")
                    .description("Test Description")
                    .taskType(TaskType.CODE)
                    .orderNum(4) // 3 + 1 = 4
                    .questionIds("[\"q1\", \"q2\"]")
                    .required(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                    
            when(experimentTaskRepository.save(any(ExperimentTask.class))).thenReturn(newTask);

            // When
            ExperimentTaskDTO result = experimentTaskService.createTask("experiment1", testTaskRequest);

            // Then
            assertNotNull(result);
            assertEquals(4, result.getOrderNum());
            verify(experimentTaskRepository).save(any(ExperimentTask.class));
        }

        @Test
        void createTask_WithNullTaskType_ShouldSetDefaultTaskType() {
            // Given
            ExperimentTaskRequestDTO requestWithoutTaskType = ExperimentTaskRequestDTO.builder()
                    .title("Test Task")
                    .description("Test Description")
                    .taskType(null)
                    .required(true)
                    .question(List.of("q1", "q2"))
                    .build();

            ExperimentTask taskWithDefaultType = ExperimentTask.builder()
                    .id("task1")
                    .experimentId("experiment1")
                    .title("Test Task")
                    .description("Test Description")
                    .taskType(TaskType.OTHER) // 默认类型
                    .orderNum(1)
                    .questionIds("[\"q1\", \"q2\"]")
                    .required(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(experimentRepository.existsById("experiment1")).thenReturn(true);
            when(experimentTaskRepository.findMaxOrderNumByExperimentId("experiment1")).thenReturn(0);
            when(experimentTaskRepository.save(any(ExperimentTask.class))).thenReturn(taskWithDefaultType);

            // When
            ExperimentTaskDTO result = experimentTaskService.createTask("experiment1", requestWithoutTaskType);

            // Then
            assertNotNull(result);
            assertEquals(TaskType.OTHER, result.getTaskType());
        }
    }

    @Nested
    class GetTasksTests {
        
        @Test
        void getTasksByExperimentId_WithExistingExperiment_ShouldReturnTasks() {
            // Given
            ExperimentTask task2 = ExperimentTask.builder()
                    .id("task2")
                    .experimentId("experiment1")
                    .title("Task 2")
                    .description("Description 2")
                    .taskType(TaskType.OTHER)
                    .orderNum(2)
                    .questionIds("[\"q3\", \"q4\"]")
                    .required(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(experimentTaskRepository.findByExperimentIdOrderByOrderNumAsc("experiment1"))
                    .thenReturn(List.of(testTask, task2));

            // When
            List<ExperimentTaskDTO> result = experimentTaskService.getTasksByExperimentId("experiment1");

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("task1", result.get(0).getId());
            assertEquals("task2", result.get(1).getId());
            assertEquals("Test Task", result.get(0).getTitle());
            assertEquals("Task 2", result.get(1).getTitle());
        }

        @Test
        void getTasksByExperimentId_WithNonExistingExperiment_ShouldReturnEmptyList() {
            // Given
            when(experimentTaskRepository.findByExperimentIdOrderByOrderNumAsc("nonexistent"))
                    .thenReturn(List.of());

            // When
            List<ExperimentTaskDTO> result = experimentTaskService.getTasksByExperimentId("nonexistent");

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class UpdateTaskTests {
        
        @Test
        void updateTask_WithValidData_ShouldUpdateSuccessfully() {
            // Given
            when(experimentTaskRepository.findById("task1")).thenReturn(Optional.of(testTask));
            
            ExperimentTask updatedTask = ExperimentTask.builder()
                    .id("task1")
                    .experimentId("experiment1")
                    .title("Updated Task")
                    .description("Updated Description")
                    .taskType(TaskType.OTHER)
                    .orderNum(1)
                    .questionIds("[\"q1\", \"q2\", \"q3\"]")
                    .required(false)
                    .createdAt(testTask.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .build();
                    
            when(experimentTaskRepository.save(any(ExperimentTask.class))).thenReturn(updatedTask);

            ExperimentTaskRequestDTO updateRequest = ExperimentTaskRequestDTO.builder()
                    .title("Updated Task")
                    .description("Updated Description")
                    .taskType(TaskType.OTHER)
                    .required(false)
                    .question(List.of("q1", "q2", "q3"))
                    .build();

            // When
            ExperimentTaskDTO result = experimentTaskService.updateTask("task1", updateRequest);

            // Then
            assertNotNull(result);
            assertEquals("task1", result.getId());
            assertEquals("Updated Task", result.getTitle());
            assertEquals("Updated Description", result.getDescription());
            assertEquals(TaskType.OTHER, result.getTaskType());
            assertFalse(result.getRequired());
            verify(experimentTaskRepository).save(any(ExperimentTask.class));
        }

        @Test
        void updateTask_WithNonExistingTask_ShouldThrowException() {
            // Given
            when(experimentTaskRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                experimentTaskService.updateTask("nonexistent", testTaskRequest);
            });
            assertEquals("任务不存在", exception.getMessage());
            verify(experimentTaskRepository, never()).save(any(ExperimentTask.class));
        }

        @Test
        void updateTask_WithNullTaskType_ShouldKeepOriginalTaskType() {
            // Given
            ExperimentTaskRequestDTO updateRequestWithoutTaskType = ExperimentTaskRequestDTO.builder()
                    .title("Updated Task")
                    .description("Updated Description")
                    .taskType(null)
                    .required(false)
                    .question(List.of("q1", "q2"))
                    .build();

            when(experimentTaskRepository.findById("task1")).thenReturn(Optional.of(testTask));
            
            ExperimentTask updatedTask = ExperimentTask.builder()
                    .id("task1")
                    .experimentId("experiment1")
                    .title("Updated Task")
                    .description("Updated Description")
                    .taskType(TaskType.CODE) // 保持原来的类型
                    .orderNum(1)
                    .questionIds("[\"q1\", \"q2\"]")
                    .required(false)
                    .createdAt(testTask.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .build();
                    
            when(experimentTaskRepository.save(any(ExperimentTask.class))).thenReturn(updatedTask);

            // When
            ExperimentTaskDTO result = experimentTaskService.updateTask("task1", updateRequestWithoutTaskType);

            // Then
            assertNotNull(result);
            assertEquals(TaskType.CODE, result.getTaskType()); // 应该保持原来的类型
            verify(experimentTaskRepository).save(any(ExperimentTask.class));
        }
    }

    @Nested
    class DeleteTaskTests {
        
        @Test
        void deleteTask_WithExistingTask_ShouldDeleteSuccessfully() {
            // Given
            when(experimentTaskRepository.existsById("task1")).thenReturn(true);

            // When
            experimentTaskService.deleteTask("task1");

            // Then
            verify(experimentTaskRepository).deleteById("task1");
        }

        @Test
        void deleteTask_WithNonExistingTask_ShouldThrowException() {
            // Given
            when(experimentTaskRepository.existsById("nonexistent")).thenReturn(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                experimentTaskService.deleteTask("nonexistent");
            });
            assertEquals("任务不存在", exception.getMessage());
            verify(experimentTaskRepository, never()).deleteById(any());
        }
    }

    @Nested
    class AdjustTaskOrderTests {
        
        @Test
        void adjustTaskOrder_WithValidData_ShouldAdjustOrderSuccessfully() {
            // Given
            ExperimentTask task2 = ExperimentTask.builder()
                    .id("task2")
                    .experimentId("experiment1")
                    .title("Task 2")
                    .description("Description 2")
                    .taskType(TaskType.OTHER)
                    .orderNum(2)
                    .required(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            List<Map<String, String>> taskOrderList = List.of(
                    Map.of("id", "task1", "order", "2"),
                    Map.of("id", "task2", "order", "1")
            );

            when(experimentRepository.existsById("experiment1")).thenReturn(true);
            when(experimentTaskRepository.findById("task1")).thenReturn(Optional.of(testTask));
            when(experimentTaskRepository.findById("task2")).thenReturn(Optional.of(task2));
            when(experimentTaskRepository.save(any(ExperimentTask.class))).thenReturn(testTask);
            when(experimentTaskRepository.findByExperimentIdOrderByOrderNumAsc("experiment1"))
                    .thenReturn(List.of(task2, testTask)); // 重新排序后的结果

            // When
            List<ExperimentTaskDTO> result = experimentTaskService.adjustTaskOrder("experiment1", taskOrderList);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(experimentTaskRepository, times(2)).save(any(ExperimentTask.class));
        }

        @Test
        void adjustTaskOrder_WithNonExistingExperiment_ShouldThrowException() {
            // Given
            List<Map<String, String>> taskOrderList = List.of(
                    Map.of("id", "task1", "order", "1")
            );
            when(experimentRepository.existsById("nonexistent")).thenReturn(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                experimentTaskService.adjustTaskOrder("nonexistent", taskOrderList);
            });
            assertEquals("实验不存在", exception.getMessage());
            verify(experimentTaskRepository, never()).save(any(ExperimentTask.class));
        }

        @Test
        void adjustTaskOrder_WithNonExistingTask_ShouldThrowException() {
            // Given
            List<Map<String, String>> taskOrderList = List.of(
                    Map.of("id", "nonexistent", "order", "1")
            );
            when(experimentRepository.existsById("experiment1")).thenReturn(true);
            when(experimentTaskRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                experimentTaskService.adjustTaskOrder("experiment1", taskOrderList);
            });
            assertEquals("任务不存在: nonexistent", exception.getMessage());
            verify(experimentTaskRepository, never()).save(any(ExperimentTask.class));
        }

        @Test
        void adjustTaskOrder_WithTaskNotBelongingToExperiment_ShouldThrowException() {
            // Given
            ExperimentTask taskFromDifferentExperiment = ExperimentTask.builder()
                    .id("task1")
                    .experimentId("experiment2") // 不同的实验ID
                    .title("Test Task")
                    .description("Test Description")
                    .taskType(TaskType.CODE)
                    .orderNum(1)
                    .required(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            List<Map<String, String>> taskOrderList = List.of(
                    Map.of("id", "task1", "order", "1")
            );

            when(experimentRepository.existsById("experiment1")).thenReturn(true);
            when(experimentTaskRepository.findById("task1")).thenReturn(Optional.of(taskFromDifferentExperiment));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                experimentTaskService.adjustTaskOrder("experiment1", taskOrderList);
            });
            assertEquals("任务不属于指定实验", exception.getMessage());
            verify(experimentTaskRepository, never()).save(any(ExperimentTask.class));
        }

        @Test
        void adjustTaskOrder_WithInvalidOrderNumber_ShouldThrowNumberFormatException() {
            // Given
            List<Map<String, String>> taskOrderList = List.of(
                    Map.of("id", "task1", "order", "invalid")
            );
            when(experimentRepository.existsById("experiment1")).thenReturn(true);

            // When & Then
            assertThrows(NumberFormatException.class, () -> {
                experimentTaskService.adjustTaskOrder("experiment1", taskOrderList);
            });
            verify(experimentTaskRepository, never()).save(any(ExperimentTask.class));
        }
    }

    @Nested
    class EdgeCaseTests {
        
        @Test
        void createTask_WithZeroMaxOrder_ShouldSetOrderToOne() {
            // Given - 当没有任务时，repository返回0
            when(experimentRepository.existsById("experiment1")).thenReturn(true);
            when(experimentTaskRepository.findMaxOrderNumByExperimentId("experiment1")).thenReturn(0);
            
            ExperimentTask taskWithOrderOne = ExperimentTask.builder()
                    .id("task1")
                    .experimentId("experiment1")
                    .title("Test Task")
                    .description("Test Description")
                    .taskType(TaskType.CODE)
                    .orderNum(1) // 0 + 1 = 1
                    .questionIds("[\"q1\", \"q2\"]")
                    .required(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                    
            when(experimentTaskRepository.save(any(ExperimentTask.class))).thenReturn(taskWithOrderOne);

            // When
            ExperimentTaskDTO result = experimentTaskService.createTask("experiment1", testTaskRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getOrderNum());
        }

        @Test
        void adjustTaskOrder_WithEmptyTaskOrderList_ShouldReturnCurrentTasks() {
            // Given
            List<Map<String, String>> emptyTaskOrderList = List.of();
            when(experimentRepository.existsById("experiment1")).thenReturn(true);
            when(experimentTaskRepository.findByExperimentIdOrderByOrderNumAsc("experiment1"))
                    .thenReturn(List.of(testTask));

            // When
            List<ExperimentTaskDTO> result = experimentTaskService.adjustTaskOrder("experiment1", emptyTaskOrderList);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(experimentTaskRepository, never()).save(any(ExperimentTask.class));
        }

        @Test
        void updateTask_WithNullQuestion_ShouldSetQuestionIdsToNull() {
            // Given
            ExperimentTaskRequestDTO updateRequestWithNullQuestion = ExperimentTaskRequestDTO.builder()
                    .title("Updated Task")
                    .description("Updated Description")
                    .taskType(TaskType.OTHER)
                    .required(false)
                    .question(null)
                    .build();

            when(experimentTaskRepository.findById("task1")).thenReturn(Optional.of(testTask));
            
            ExperimentTask updatedTask = ExperimentTask.builder()
                    .id("task1")
                    .experimentId("experiment1")
                    .title("Updated Task")
                    .description("Updated Description")
                    .taskType(TaskType.OTHER)
                    .orderNum(1)
                    .questionIds(null) // 应该是null
                    .required(false)
                    .createdAt(testTask.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .build();
                    
            when(experimentTaskRepository.save(any(ExperimentTask.class))).thenReturn(updatedTask);

            // When
            ExperimentTaskDTO result = experimentTaskService.updateTask("task1", updateRequestWithNullQuestion);

            // Then
            assertNotNull(result);
            verify(experimentTaskRepository).save(any(ExperimentTask.class));
        }
    }
}
