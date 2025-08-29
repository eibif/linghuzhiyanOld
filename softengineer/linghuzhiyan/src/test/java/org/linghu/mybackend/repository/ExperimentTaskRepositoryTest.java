package org.linghu.mybackend.repository;

import org.junit.jupiter.api.Test;
import org.linghu.mybackend.domain.ExperimentTask;
import org.linghu.mybackend.constants.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExperimentTaskRepository 测试类
 */
@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = {"/schema.sql", "/data.sql"})
class ExperimentTaskRepositoryTest {

    @Autowired
    private ExperimentTaskRepository taskRepository;

    @Test
    void findByExperimentId_WithExistingExperimentId_ShouldReturnTasks() {
        // When
        List<ExperimentTask> tasks = taskRepository.findByExperimentId("experiment1");

        // Then
        assertNotNull(tasks);
        assertEquals(3, tasks.size());
        assertTrue(tasks.stream().allMatch(t -> "experiment1".equals(t.getExperimentId())));
    }

    @Test
    void findByExperimentId_WithNonExistingExperimentId_ShouldReturnEmptyList() {
        // When
        List<ExperimentTask> tasks = taskRepository.findByExperimentId("nonexistent");

        // Then
        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }

    @Test
    void findByExperimentIdOrderByOrderNum_WithExistingExperimentId_ShouldReturnOrderedTasks() {
        // When
        List<ExperimentTask> tasks = taskRepository.findByExperimentIdOrderByOrderNum("experiment1");

        // Then
        assertNotNull(tasks);
        assertEquals(3, tasks.size());
        assertEquals(1, tasks.get(0).getOrderNum());
        assertEquals(2, tasks.get(1).getOrderNum());
        assertEquals(3, tasks.get(2).getOrderNum());
    }

    @Test
    void findByExperimentIdAndId_WithExistingTaskAndExperiment_ShouldReturnTask() {
        // When
        Optional<ExperimentTask> task = taskRepository.findByExperimentIdAndId("experiment1", "task1");

        // Then
        assertTrue(task.isPresent());
        assertEquals("task1", task.get().getId());
        assertEquals("experiment1", task.get().getExperimentId());
    }

    @Test
    void findByExperimentIdAndId_WithNonExistingTask_ShouldReturnEmpty() {
        // When
        Optional<ExperimentTask> task = taskRepository.findByExperimentIdAndId("experiment1", "nonexistent");

        // Then
        assertFalse(task.isPresent());
    }

    @Test
    void findByExperimentIdAndId_WithWrongExperiment_ShouldReturnEmpty() {
        // When
        Optional<ExperimentTask> task = taskRepository.findByExperimentIdAndId("wrongexperiment", "task1");

        // Then
        assertFalse(task.isPresent());
    }

    @Test
    void save_WithValidTask_ShouldSaveSuccessfully() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        ExperimentTask newTask = ExperimentTask.builder()
                .id(UUID.randomUUID().toString())
                .experimentId("experiment1")
                .title("New Task")
                .description("Task description")
                .orderNum(1)
                .required(true)
                .taskType(TaskType.OTHER)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // When
        ExperimentTask saved = taskRepository.save(newTask);

        // Then
        assertNotNull(saved);
        assertEquals("experiment1", saved.getExperimentId());
        assertEquals("New Task", saved.getTitle());
        assertTrue(saved.getRequired());
        assertEquals(1, saved.getOrderNum());
    }

    @Test
    void findById_WithExistingId_ShouldReturnTask() {
        // When
        Optional<ExperimentTask> found = taskRepository.findById("task1");

        // Then
        assertTrue(found.isPresent());
        assertEquals("task1", found.get().getId());
        assertEquals("experiment1", found.get().getExperimentId());
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // When
        Optional<ExperimentTask> found = taskRepository.findById("nonexistent");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void delete_WithExistingTask_ShouldDeleteSuccessfully() {
        // Given
        Optional<ExperimentTask> task = taskRepository.findById("task1");
        assertTrue(task.isPresent());

        // When
        taskRepository.delete(task.get());

        // Then
        Optional<ExperimentTask> deleted = taskRepository.findById("task1");
        assertFalse(deleted.isPresent());
    }

    @Test
    void deleteById_WithExistingId_ShouldDeleteSuccessfully() {
        // Given
        assertTrue(taskRepository.existsById("task2"));

        // When
        taskRepository.deleteById("task2");

        // Then
        assertFalse(taskRepository.existsById("task2"));
    }

    @Test
    void update_ExistingTask_ShouldUpdateSuccessfully() {
        // Given - 先获取现有任务
        Optional<ExperimentTask> existing = taskRepository.findById("task1");
        assertTrue(existing.isPresent());
        
        ExperimentTask task = existing.get();
        task.setRequired(false);
        task.setOrderNum(10);

        // When
        ExperimentTask updated = taskRepository.save(task);

        // Then
        assertNotNull(updated);
        assertFalse(updated.getRequired());
        assertEquals(10, updated.getOrderNum());
    }

    @Test
    void findAll_ShouldReturnAllTasks() {
        // When
        List<ExperimentTask> tasks = taskRepository.findAll();

        // Then
        assertNotNull(tasks);
        assertEquals(4, tasks.size());
    }

    @Test
    void existsById_WithExistingId_ShouldReturnTrue() {
        // When
        boolean exists = taskRepository.existsById("task1");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsById_WithNonExistingId_ShouldReturnFalse() {
        // When
        boolean exists = taskRepository.existsById("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // When
        long count = taskRepository.count();

        // Then
        assertEquals(4, count);
    }

    @Test
    void findByExperimentIdOrderByOrderNumAsc_ShouldReturnTasksInAscendingOrder() {
        // When
        List<ExperimentTask> tasks = taskRepository.findByExperimentIdOrderByOrderNumAsc("experiment1");

        // Then
        assertNotNull(tasks);
        assertEquals(3, tasks.size());
        // 验证顺序递增
        for (int i = 0; i < tasks.size() - 1; i++) {
            assertTrue(tasks.get(i).getOrderNum() <= tasks.get(i + 1).getOrderNum());
        }
    }

    @Test
    void findByExperimentIdAndRequiredTrue_WithExistingExperiment_ShouldReturnRequiredTasks() {
        // When
        List<ExperimentTask> tasks = taskRepository.findByExperimentIdAndRequiredTrue("experiment1");

        // Then
        assertNotNull(tasks);
        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().allMatch(ExperimentTask::getRequired));
    }

    @Test
    void findByExperimentIdAndRequiredFalse_WithExistingExperiment_ShouldReturnOptionalTasks() {
        // When
        List<ExperimentTask> tasks = taskRepository.findByExperimentIdAndRequiredFalse("experiment1");

        // Then
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        assertFalse(tasks.get(0).getRequired());
    }

    @Test
    void countByExperimentId_WithExistingExperiment_ShouldReturnCorrectCount() {
        // When
        long count = taskRepository.countByExperimentId("experiment1");

        // Then
        assertEquals(3, count);
    }

    @Test
    void countByExperimentId_WithNonExistingExperiment_ShouldReturnZero() {
        // When
        long count = taskRepository.countByExperimentId("nonexistent");

        // Then
        assertEquals(0, count);
    }

    @Test
    void countByExperimentIdAndRequiredTrue_WithExistingExperiment_ShouldReturnCorrectCount() {
        // When
        long count = taskRepository.countByExperimentIdAndRequiredTrue("experiment1");

        // Then
        assertEquals(2, count);
    }

    @Test
    void countByExperimentIdAndRequiredTrue_WithNonExistingExperiment_ShouldReturnZero() {
        // When
        long count = taskRepository.countByExperimentIdAndRequiredTrue("nonexistent");

        // Then
        assertEquals(0, count);
    }

    @Test
    void findMaxOrderNumByExperimentId_WithExistingExperiment_ShouldReturnMaxOrderNum() {
        // When
        int maxOrderNum = taskRepository.findMaxOrderNumByExperimentId("experiment1");

        // Then
        assertEquals(3, maxOrderNum);
    }

    @Test
    void findMaxOrderNumByExperimentId_WithEmptyExperiment_ShouldReturnZero() {
        // When
        int maxOrderNum = taskRepository.findMaxOrderNumByExperimentId("nonexistent");

        // Then
        assertEquals(0, maxOrderNum);
    }

    @Test
    void findByExperimentIdAndTitle_WithExistingTaskAndTitle_ShouldReturnTask() {
        // When
        Optional<ExperimentTask> task = taskRepository.findByExperimentIdAndTitle("experiment1", "Task 1");

        // Then
        assertTrue(task.isPresent());
        assertEquals("Task 1", task.get().getTitle());
        assertEquals("experiment1", task.get().getExperimentId());
    }

    @Test
    void findByExperimentIdAndTitle_WithNonExistingTitle_ShouldReturnEmpty() {
        // When
        Optional<ExperimentTask> task = taskRepository.findByExperimentIdAndTitle("experiment1", "Nonexistent Task");

        // Then
        assertFalse(task.isPresent());
    }

    @Test
    void deleteByExperimentId_WithExistingExperiment_ShouldDeleteAllTasksAndReturnCount() {
        // When
        long deletedCount = taskRepository.deleteByExperimentId("experiment1");

        // Then
        assertEquals(3, deletedCount);
        List<ExperimentTask> remaining = taskRepository.findByExperimentId("experiment1");
        assertTrue(remaining.isEmpty());
    }

    @Test
    void deleteByExperimentId_WithNonExistingExperiment_ShouldReturnZero() {
        // When
        long deletedCount = taskRepository.deleteByExperimentId("nonexistent");

        // Then
        assertEquals(0, deletedCount);
    }
}
