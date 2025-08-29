package org.linghu.mybackend.repository;

import org.junit.jupiter.api.Test;
import org.linghu.mybackend.domain.ExperimentSubmission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExperimentSubmissionRepository 测试类
 */
@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = {"/schema.sql", "/data.sql"})
class ExperimentSubmissionRepositoryTest {

    @Autowired
    private ExperimentSubmissionRepository submissionRepository;

    @Test
    void findByTaskId_WithExistingTaskId_ShouldReturnSubmissions() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findByTaskId("task1");

        // Then
        assertNotNull(submissions);
        assertEquals(3, submissions.size());
        assertTrue(submissions.stream().allMatch(s -> "task1".equals(s.getTaskId())));
    }

    @Test
    void findByTaskId_WithNonExistingTaskId_ShouldReturnEmptyList() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findByTaskId("nonexistent");

        // Then
        assertNotNull(submissions);
        assertTrue(submissions.isEmpty());
    }

    @Test
    void findByTaskIdWithPageable_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<ExperimentSubmission> submissions = submissionRepository.findByTaskId("task1", pageable);

        // Then
        assertNotNull(submissions);
        assertEquals(2, submissions.getContent().size());
        assertEquals(3, submissions.getTotalElements());
        assertEquals(2, submissions.getTotalPages());
    }

    @Test
    void findByUserId_WithExistingUserId_ShouldReturnSubmissions() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findByUserId("user1");

        // Then
        assertNotNull(submissions);
        assertEquals(3, submissions.size());
        assertTrue(submissions.stream().allMatch(s -> "user1".equals(s.getUserId())));
    }

    @Test
    void findByUserId_WithNonExistingUserId_ShouldReturnEmptyList() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findByUserId("nonexistent");

        // Then
        assertNotNull(submissions);
        assertTrue(submissions.isEmpty());
    }

    @Test
    void findByUserIdWithPageable_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<ExperimentSubmission> submissions = submissionRepository.findByUserId("user1", pageable);

        // Then
        assertNotNull(submissions);
        assertEquals(1, submissions.getContent().size());
        assertEquals(3, submissions.getTotalElements());
        assertEquals(3, submissions.getTotalPages());
    }

    @Test
    void findByTaskIdAndUserId_WithExistingIds_ShouldReturnSubmission() {
        // When
        Optional<ExperimentSubmission> submission = submissionRepository.findByTaskIdAndUserId("task1", "user1");

        // Then
        assertTrue(submission.isPresent());
        assertEquals("task1", submission.get().getTaskId());
        assertEquals("user1", submission.get().getUserId());
    }

    @Test
    void findByTaskIdAndUserId_WithNonExistingIds_ShouldReturnEmpty() {
        // When
        Optional<ExperimentSubmission> submission = submissionRepository.findByTaskIdAndUserId("nonexistent", "user1");

        // Then
        assertFalse(submission.isPresent());
    }

    @Test
    void findFirstByUserIdOrderBySubmitTimeDesc_WithExistingUserId_ShouldReturnLatestSubmission() {
        // When
        Optional<ExperimentSubmission> submission = submissionRepository.findFirstByUserIdOrderBySubmitTimeDesc("user1");

        // Then
        assertTrue(submission.isPresent());
        assertEquals("user1", submission.get().getUserId());
    }

    @Test
    void findFirstByUserIdOrderBySubmitTimeDesc_WithNonExistingUserId_ShouldReturnEmpty() {
        // When
        Optional<ExperimentSubmission> submission = submissionRepository.findFirstByUserIdOrderBySubmitTimeDesc("nonexistent");

        // Then
        assertFalse(submission.isPresent());
    }

    @Test
    void findByTaskIdAndSubmitTimeBetween_WithValidTimeRange_ShouldReturnSubmissions() {
        // Given
        Date startDate = new Date(System.currentTimeMillis() - 86400000); // 1 day ago
        Date endDate = new Date(System.currentTimeMillis() + 86400000); // 1 day later

        // When
        List<ExperimentSubmission> submissions = submissionRepository.findByTaskIdAndSubmitTimeBetween("task1", startDate, endDate);

        // Then
        assertNotNull(submissions);
        assertEquals(3, submissions.size());
    }

    @Test
    void findByTaskIdAndSubmitTimeBetween_WithEmptyTimeRange_ShouldReturnEmptyList() {
        // Given
        Date startDate = new Date(System.currentTimeMillis() - 86400000 * 10); // 10 days ago
        Date endDate = new Date(System.currentTimeMillis() - 86400000 * 9); // 9 days ago

        // When
        List<ExperimentSubmission> submissions = submissionRepository.findByTaskIdAndSubmitTimeBetween("task1", startDate, endDate);

        // Then
        assertNotNull(submissions);
        assertTrue(submissions.isEmpty());
    }

    @Test
    void findByGraderId_WithExistingGraderId_ShouldReturnSubmissions() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findByGraderId("grader1");

        // Then
        assertNotNull(submissions);
        assertEquals(2, submissions.size());
        assertTrue(submissions.stream().allMatch(s -> "grader1".equals(s.getGraderId())));
    }

    @Test
    void findByGraderId_WithNonExistingGraderId_ShouldReturnEmptyList() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findByGraderId("nonexistent");

        // Then
        assertNotNull(submissions);
        assertTrue(submissions.isEmpty());
    }

    @Test
    void findUngradedSubmissionsByTaskId_ShouldReturnUngradedSubmissions() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findUngradedSubmissionsByTaskId("task2");

        // Then
        assertNotNull(submissions);
        assertEquals(1, submissions.size());
        assertNull(submissions.get(0).getGraderId());
    }

    @Test
    void findUngradedSubmissionsByTaskId_WithNoUngradedSubmissions_ShouldReturnEmptyList() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findUngradedSubmissionsByTaskId("task1");

        // Then
        assertNotNull(submissions);
        assertTrue(submissions.isEmpty());
    }

    @Test
    void findGradedSubmissionsByTaskId_ShouldReturnGradedSubmissions() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findGradedSubmissionsByTaskId("task1");

        // Then
        assertNotNull(submissions);
        assertEquals(3, submissions.size());
        assertTrue(submissions.stream().allMatch(s -> s.getGraderId() != null));
    }

    @Test
    void findGradedSubmissionsByTaskId_WithNoGradedSubmissions_ShouldReturnEmptyList() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findGradedSubmissionsByTaskId("task3");

        // Then
        assertNotNull(submissions);
        assertTrue(submissions.isEmpty());
    }

    @Test
    void findBySubmitTimeBetween_WithValidTimeRange_ShouldReturnSubmissions() {
        // Given
        Date startDate = new Date(System.currentTimeMillis() - 86400000); // 1 day ago
        Date endDate = new Date(System.currentTimeMillis() + 86400000); // 1 day later

        // When
        List<ExperimentSubmission> submissions = submissionRepository.findBySubmitTimeBetween(startDate, endDate);

        // Then
        assertNotNull(submissions);
        assertEquals(4, submissions.size());
    }

    @Test
    void findBySubmitTimeBetween_WithEmptyTimeRange_ShouldReturnEmptyList() {
        // Given
        Date startDate = new Date(System.currentTimeMillis() - 86400000 * 10); // 10 days ago
        Date endDate = new Date(System.currentTimeMillis() - 86400000 * 9); // 9 days ago

        // When
        List<ExperimentSubmission> submissions = submissionRepository.findBySubmitTimeBetween(startDate, endDate);

        // Then
        assertNotNull(submissions);
        assertTrue(submissions.isEmpty());
    }

    @Test
    void findByTaskIdAndUserAnswerContaining_WithExistingContent_ShouldReturnSubmissions() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findByTaskIdAndUserAnswerContaining("task1", "solution");

        // Then
        assertNotNull(submissions);
        assertEquals(3, submissions.size());
        assertTrue(submissions.stream().allMatch(s -> s.getUserAnswer().contains("solution")));
    }

    @Test
    void findByTaskIdAndUserAnswerContaining_WithNonExistingContent_ShouldReturnEmptyList() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findByTaskIdAndUserAnswerContaining("task1", "nonexistent");

        // Then
        assertNotNull(submissions);
        assertTrue(submissions.isEmpty());
    }

    @Test
    void getAverageScoreByTaskId_WithScoredSubmissions_ShouldReturnAverageScore() {
        // When
        Double averageScore = submissionRepository.getAverageScoreByTaskId("task1");

        // Then
        assertNotNull(averageScore);
        // (85.50 + 92.00 + 95.00) / 3 = 90.83
        assertEquals(90.83, averageScore, 0.01);
    }

    @Test
    void getAverageScoreByTaskId_WithNoScoredSubmissions_ShouldReturnNull() {
        // When
        Double averageScore = submissionRepository.getAverageScoreByTaskId("task2");

        // Then
        assertNull(averageScore);
    }

    @Test
    void findByUserIdAndExperimentIdOrderBySubmitTimeDesc_WithExistingUserAndExperiment_ShouldReturnSubmissions() {
        // When & Then
        assertDoesNotThrow(() -> {
            List<ExperimentSubmission> submissions = submissionRepository.findByUserIdAndExperimentIdOrderBySubmitTimeDesc("user1", "experiment1");
            assertNotNull(submissions);
        });
    }

    @Test
    void save_WithValidSubmission_ShouldSaveSuccessfully() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        ExperimentSubmission newSubmission = ExperimentSubmission.builder()
                .id(UUID.randomUUID().toString())
                .taskId("task3")
                .userId("user3")
                .userAnswer("{\"answer\": \"new_solution\"}")
                .score(null)
                .submitTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // When
        ExperimentSubmission saved = submissionRepository.save(newSubmission);

        // Then
        assertNotNull(saved);
        assertEquals("task3", saved.getTaskId());
        assertEquals("user3", saved.getUserId());
        assertEquals("{\"answer\": \"new_solution\"}", saved.getUserAnswer());
    }

    @Test
    void findById_WithExistingId_ShouldReturnSubmission() {
        // When
        Optional<ExperimentSubmission> found = submissionRepository.findById("sub1");

        // Then
        assertTrue(found.isPresent());
        assertEquals("sub1", found.get().getId());
        assertEquals("task1", found.get().getTaskId());
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // When
        Optional<ExperimentSubmission> found = submissionRepository.findById("nonexistent");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void deleteById_WithExistingId_ShouldDeleteSuccessfully() {
        // Given
        String idToDelete = "sub1";

        // When
        submissionRepository.deleteById(idToDelete);

        // Then
        Optional<ExperimentSubmission> deleted = submissionRepository.findById(idToDelete);
        assertFalse(deleted.isPresent());
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // When
        long count = submissionRepository.count();

        // Then
        assertEquals(4, count);
    }

    @Test
    void save_UpdateExistingSubmission_ShouldUpdateSuccessfully() {
        // Given
        Optional<ExperimentSubmission> existing = submissionRepository.findById("sub3");
        assertTrue(existing.isPresent());

        ExperimentSubmission submission = existing.get();
        submission.setScore(new BigDecimal("78.50"));
        submission.setUserAnswer("{\"answer\": \"updated_solution\"}");
        submission.setGraderId("grader3");
        submission.setGradedTime(LocalDateTime.now());

        // When
        ExperimentSubmission updated = submissionRepository.save(submission);

        // Then
        assertNotNull(updated);
        assertEquals(new BigDecimal("78.50"), updated.getScore());
        assertEquals("{\"answer\": \"updated_solution\"}", updated.getUserAnswer());
        assertEquals("grader3", updated.getGraderId());
        assertNotNull(updated.getGradedTime());
    }

    @Test
    void findAll_ShouldReturnAllSubmissions() {
        // When
        List<ExperimentSubmission> submissions = submissionRepository.findAll();

        // Then
        assertNotNull(submissions);
        assertEquals(4, submissions.size());
    }

    @Test
    void existsById_WithExistingId_ShouldReturnTrue() {
        // When
        boolean exists = submissionRepository.existsById("sub1");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsById_WithNonExistingId_ShouldReturnFalse() {
        // When
        boolean exists = submissionRepository.existsById("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void deleteAll_ShouldDeleteAllSubmissions() {
        // When
        submissionRepository.deleteAll();

        // Then
        long count = submissionRepository.count();
        assertEquals(0, count);
    }
}
