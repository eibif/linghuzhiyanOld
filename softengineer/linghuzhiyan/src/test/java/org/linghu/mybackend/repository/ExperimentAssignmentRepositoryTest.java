package org.linghu.mybackend.repository;

import org.junit.jupiter.api.Test;
import org.linghu.mybackend.domain.ExperimentAssignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExperimentAssignmentRepository 测试类
 */
@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = {"/schema.sql", "/data.sql"})
class ExperimentAssignmentRepositoryTest {

    @Autowired
    private ExperimentAssignmentRepository assignmentRepository;

    @Test
    void findByTaskId_WithExistingTaskId_ShouldReturnAssignments() {
        // When
        List<ExperimentAssignment> assignments = assignmentRepository.findByTaskId("task1");

        // Then
        assertNotNull(assignments);
        assertEquals(2, assignments.size());
        assertTrue(assignments.stream().allMatch(a -> "task1".equals(a.getTaskId())));
    }

    @Test
    void findByTaskId_WithNonExistingTaskId_ShouldReturnEmptyList() {
        // When
        List<ExperimentAssignment> assignments = assignmentRepository.findByTaskId("nonexistent");

        // Then
        assertNotNull(assignments);
        assertTrue(assignments.isEmpty());
    }

    @Test
    void findByUserId_WithExistingUserId_ShouldReturnAssignments() {
        // When
        List<ExperimentAssignment> assignments = assignmentRepository.findByUserId("user1");

        // Then
        assertNotNull(assignments);
        assertEquals(2, assignments.size());
        assertTrue(assignments.stream().allMatch(a -> "user1".equals(a.getUserId())));
    }

    @Test
    void findByUserId_WithNonExistingUserId_ShouldReturnEmptyList() {
        // When
        List<ExperimentAssignment> assignments = assignmentRepository.findByUserId("nonexistent");

        // Then
        assertNotNull(assignments);
        assertTrue(assignments.isEmpty());
    }

    @Test
    void findByTaskIdAndUserId_WithExistingTaskAndUser_ShouldReturnAssignment() {
        // When
        Optional<ExperimentAssignment> assignment = assignmentRepository.findByTaskIdAndUserId("task1", "user1");

        // Then
        assertTrue(assignment.isPresent());
        assertEquals("task1", assignment.get().getTaskId());
        assertEquals("user1", assignment.get().getUserId());
    }

    @Test
    void findByTaskIdAndUserId_WithNonExistingTaskAndUser_ShouldReturnEmpty() {
        // When
        Optional<ExperimentAssignment> assignment = assignmentRepository.findByTaskIdAndUserId("nonexistent", "user1");

        // Then
        assertFalse(assignment.isPresent());
    }

    @Test
    void existsByTaskIdAndUserId_WithExistingTaskAndUser_ShouldReturnTrue() {
        // When
        boolean exists = assignmentRepository.existsByTaskIdAndUserId("task1", "user1");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByTaskIdAndUserId_WithNonExistingTaskAndUser_ShouldReturnFalse() {
        // When
        boolean exists = assignmentRepository.existsByTaskIdAndUserId("nonexistent", "user1");

        // Then
        assertFalse(exists);
    }

    @Test
    void save_WithValidAssignment_ShouldSaveSuccessfully() {
        // Given
        Date now = new Date();
        ExperimentAssignment newAssignment = ExperimentAssignment.builder()
                .id(UUID.randomUUID().toString())
                .taskId("task3")
                .userId("user3")
                .assignedAt(now)
                .build();

        // When
        ExperimentAssignment saved = assignmentRepository.save(newAssignment);

        // Then
        assertNotNull(saved);
        assertEquals("task3", saved.getTaskId());
        assertEquals("user3", saved.getUserId());
        assertNotNull(saved.getAssignedAt());
    }

    @Test
    void findById_WithExistingId_ShouldReturnAssignment() {
        // When
        Optional<ExperimentAssignment> found = assignmentRepository.findById("assign1");

        // Then
        assertTrue(found.isPresent());
        assertEquals("assign1", found.get().getId());
        assertEquals("task1", found.get().getTaskId());
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // When
        Optional<ExperimentAssignment> found = assignmentRepository.findById("nonexistent");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void deleteById_WithExistingId_ShouldDeleteSuccessfully() {
        // Given
        String idToDelete = "assign1";

        // When
        assignmentRepository.deleteById(idToDelete);

        // Then
        Optional<ExperimentAssignment> deleted = assignmentRepository.findById(idToDelete);
        assertFalse(deleted.isPresent());
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // When
        long count = assignmentRepository.count();

        // Then
        assertEquals(3, count);
    }

    @Test
    void save_UpdateExistingAssignment_ShouldUpdateSuccessfully() {
        // Given - 先获取现有的分配
        Optional<ExperimentAssignment> existing = assignmentRepository.findById("assign1");
        assertTrue(existing.isPresent());
        
        ExperimentAssignment assignment = existing.get();
        Date newAssignedTime = new Date(System.currentTimeMillis() + 86400000L); // +1 day
        assignment.setAssignedAt(newAssignedTime);

        // When
        ExperimentAssignment updated = assignmentRepository.save(assignment);

        // Then
        assertNotNull(updated);
        assertEquals(newAssignedTime, updated.getAssignedAt());
    }

    @Test
    void findAll_ShouldReturnAllAssignments() {
        // When
        List<ExperimentAssignment> assignments = assignmentRepository.findAll();

        // Then
        assertNotNull(assignments);
        assertEquals(3, assignments.size());
    }

    @Test
    void existsById_WithExistingId_ShouldReturnTrue() {
        // When
        boolean exists = assignmentRepository.existsById("assign1");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsById_WithNonExistingId_ShouldReturnFalse() {
        // When
        boolean exists = assignmentRepository.existsById("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void deleteAll_ShouldDeleteAllAssignments() {
        // When
        assignmentRepository.deleteAll();

        // Then
        long count = assignmentRepository.count();
        assertEquals(0, count);
    }

    @Test
    void countByTaskId_WithExistingTaskId_ShouldReturnCorrectCount() {
        // When
        long count = assignmentRepository.countByTaskId("task1");

        // Then
        assertEquals(2, count);
    }

    @Test
    void countByTaskId_WithNonExistingTaskId_ShouldReturnZero() {
        // When
        long count = assignmentRepository.countByTaskId("nonexistent");

        // Then
        assertEquals(0, count);
    }

    @Test
    void deleteByTaskId_WithExistingTaskId_ShouldDeleteAllAssignmentsAndReturnCount() {
        // When
        long deletedCount = assignmentRepository.deleteByTaskId("task1");

        // Then
        assertEquals(2, deletedCount);
        List<ExperimentAssignment> remaining = assignmentRepository.findByTaskId("task1");
        assertTrue(remaining.isEmpty());
    }

    @Test
    void deleteByTaskId_WithNonExistingTaskId_ShouldReturnZero() {
        // When
        long deletedCount = assignmentRepository.deleteByTaskId("nonexistent");

        // Then
        assertEquals(0, deletedCount);
    }

    @Test
    void deleteByTaskIdAndUserId_WithExistingTaskAndUser_ShouldDeleteAndReturnCount() {
        // When
        long deletedCount = assignmentRepository.deleteByTaskIdAndUserId("task1", "user1");

        // Then
        assertEquals(1, deletedCount);
        Optional<ExperimentAssignment> assignment = assignmentRepository.findByTaskIdAndUserId("task1", "user1");
        assertFalse(assignment.isPresent());
    }

    @Test
    void deleteByTaskIdAndUserId_WithNonExistingTaskAndUser_ShouldReturnZero() {
        // When
        long deletedCount = assignmentRepository.deleteByTaskIdAndUserId("nonexistent", "user1");

        // Then
        assertEquals(0, deletedCount);
    }
}
