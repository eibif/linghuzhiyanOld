package org.linghu.mybackend.repository;

import org.junit.jupiter.api.Test;
import org.linghu.mybackend.domain.ExperimentEvaluation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExperimentEvaluationRepository 测试类
 */
@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = {"/schema.sql", "/data.sql"})
class ExperimentEvaluationRepositoryTest {

    @Autowired
    private ExperimentEvaluationRepository evaluationRepository;

    @Test
    void findBySubmissionId_WithExistingSubmissionId_ShouldReturnEvaluations() {
        // When
        List<ExperimentEvaluation> evaluations = evaluationRepository.findBySubmissionId("sub1");

        // Then
        assertNotNull(evaluations);
        assertEquals(1, evaluations.size());
        assertTrue(evaluations.stream().allMatch(e -> "sub1".equals(e.getSubmissionId())));
    }

    @Test
    void findBySubmissionId_WithNonExistingSubmissionId_ShouldReturnEmptyList() {
        // When
        List<ExperimentEvaluation> evaluations = evaluationRepository.findBySubmissionId("nonexistent");

        // Then
        assertNotNull(evaluations);
        assertTrue(evaluations.isEmpty());
    }

    @Test
    void findFirstBySubmissionIdOrderByIdDesc_WithExistingSubmissionId_ShouldReturnLatestEvaluation() {
        // When
        Optional<ExperimentEvaluation> evaluation = evaluationRepository.findFirstBySubmissionIdOrderByIdDesc("sub1");

        // Then
        assertTrue(evaluation.isPresent());
        assertEquals("sub1", evaluation.get().getSubmissionId());
    }

    @Test
    void findFirstBySubmissionIdOrderByIdDesc_WithNonExistingSubmissionId_ShouldReturnEmpty() {
        // When
        Optional<ExperimentEvaluation> evaluation = evaluationRepository.findFirstBySubmissionIdOrderByIdDesc("nonexistent");

        // Then
        assertFalse(evaluation.isPresent());
    }

    @Test
    void findByUserIdAndTaskIdOrderByIdDesc_WithExistingUserAndTask_ShouldReturnEvaluationsInDescOrder() {
        // When
        List<ExperimentEvaluation> evaluations = evaluationRepository.findByUserIdAndTaskIdOrderByIdDesc("user1", "task1");

        // Then
        assertNotNull(evaluations);
        assertEquals(2, evaluations.size());
        assertTrue(evaluations.stream().allMatch(e -> "user1".equals(e.getUserId()) && "task1".equals(e.getTaskId())));
    }

    @Test
    void findByUserIdAndTaskIdOrderByIdDesc_WithNonExistingUserAndTask_ShouldReturnEmptyList() {
        // When
        List<ExperimentEvaluation> evaluations = evaluationRepository.findByUserIdAndTaskIdOrderByIdDesc("nonexistent", "task1");

        // Then
        assertNotNull(evaluations);
        assertTrue(evaluations.isEmpty());
    }

    @Test
    void save_WithValidEvaluation_ShouldSaveSuccessfully() {
        // Given
        ExperimentEvaluation newEvaluation = ExperimentEvaluation.builder()
                .id(UUID.randomUUID().toString())
                .submissionId("sub3")
                .userId("user3")
                .taskId("task3")
                .score(new BigDecimal("88.50"))
                .errorMessage(null)
                .additionalInfo("Evaluation completed successfully")
                .build();

        // When
        ExperimentEvaluation saved = evaluationRepository.save(newEvaluation);

        // Then
        assertNotNull(saved);
        assertEquals("sub3", saved.getSubmissionId());
        assertEquals("user3", saved.getUserId());
        assertEquals("task3", saved.getTaskId());
        assertEquals(new BigDecimal("88.50"), saved.getScore());
        assertEquals("Evaluation completed successfully", saved.getAdditionalInfo());
    }

    @Test
    void findById_WithExistingId_ShouldReturnEvaluation() {
        // When
        Optional<ExperimentEvaluation> found = evaluationRepository.findById("eval1");

        // Then
        assertTrue(found.isPresent());
        assertEquals("eval1", found.get().getId());
        assertEquals("sub1", found.get().getSubmissionId());
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // When
        Optional<ExperimentEvaluation> found = evaluationRepository.findById("nonexistent");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void deleteById_WithExistingId_ShouldDeleteSuccessfully() {
        // Given
        String idToDelete = "eval1";

        // When
        evaluationRepository.deleteById(idToDelete);

        // Then
        Optional<ExperimentEvaluation> deleted = evaluationRepository.findById(idToDelete);
        assertFalse(deleted.isPresent());
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // When
        long count = evaluationRepository.count();

        // Then
        assertEquals(3, count);
    }

    @Test
    void save_UpdateExistingEvaluation_ShouldUpdateSuccessfully() {
        // Given
        Optional<ExperimentEvaluation> existing = evaluationRepository.findById("eval1");
        assertTrue(existing.isPresent());

        ExperimentEvaluation testEvaluation1 = existing.get();
        testEvaluation1.setScore(new BigDecimal("95.00"));
        testEvaluation1.setAdditionalInfo("Updated evaluation feedback");
        testEvaluation1.setErrorMessage(null);

        // When
        ExperimentEvaluation updated = evaluationRepository.save(testEvaluation1);

        // Then
        assertNotNull(updated);
        assertEquals(new BigDecimal("95.00"), updated.getScore());
        assertEquals("Updated evaluation feedback", updated.getAdditionalInfo());
        assertNull(updated.getErrorMessage());
    }

    @Test
    void findAll_ShouldReturnAllEvaluations() {
        // When
        List<ExperimentEvaluation> evaluations = evaluationRepository.findAll();

        // Then
        assertNotNull(evaluations);
        assertEquals(3, evaluations.size());
    }

    @Test
    void existsById_WithExistingId_ShouldReturnTrue() {
        // When
        boolean exists = evaluationRepository.existsById("eval1");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsById_WithNonExistingId_ShouldReturnFalse() {
        // When
        boolean exists = evaluationRepository.existsById("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void deleteAll_ShouldDeleteAllEvaluations() {
        // When
        evaluationRepository.deleteAll();

        // Then
        long count = evaluationRepository.count();
        assertEquals(0, count);
    }

    @Test
    void save_WithErrorMessage_ShouldSaveSuccessfully() {
        // Given
        ExperimentEvaluation errorEvaluation = ExperimentEvaluation.builder()
                .id(UUID.randomUUID().toString())
                .submissionId("sub4")
                .userId("user4")
                .taskId("task4")
                .score(new BigDecimal("0"))
                .errorMessage("Compilation error: missing semicolon")
                .additionalInfo("Error occurred during code compilation")
                .build();

        // When
        ExperimentEvaluation saved = evaluationRepository.save(errorEvaluation);

        // Then
        assertNotNull(saved);
        assertEquals("sub4", saved.getSubmissionId());
        assertEquals("Compilation error: missing semicolon", saved.getErrorMessage());
        assertEquals("Error occurred during code compilation", saved.getAdditionalInfo());
        assertEquals(new BigDecimal("0"), saved.getScore());
    }

    @Test
    void save_WithNullScore_ShouldSaveSuccessfully() {
        // Given
        ExperimentEvaluation pendingEvaluation = ExperimentEvaluation.builder()
                .id(UUID.randomUUID().toString())
                .submissionId("sub5")
                .userId("user5")
                .taskId("task5")
                .score(null)
                .errorMessage(null)
                .additionalInfo("Evaluation in progress")
                .build();

        // When
        ExperimentEvaluation saved = evaluationRepository.save(pendingEvaluation);

        // Then
        assertNotNull(saved);
        assertEquals("sub5", saved.getSubmissionId());
        assertNull(saved.getScore());
        assertNull(saved.getErrorMessage());
        assertEquals("Evaluation in progress", saved.getAdditionalInfo());
    }

    @Test
    void findBySubmissionId_WithMultipleEvaluations_ShouldReturnAllEvaluations() {
        // When
        List<ExperimentEvaluation> evaluations = evaluationRepository.findBySubmissionId("sub1");

        // Then
        assertNotNull(evaluations);
        assertEquals(1, evaluations.size());
        assertTrue(evaluations.stream().allMatch(e -> "sub1".equals(e.getSubmissionId())));
    }

    @Test
    void update_ExistingEvaluationScore_ShouldUpdateSuccessfully() {
        // Given
        Optional<ExperimentEvaluation> existing = evaluationRepository.findById("eval2");
        assertTrue(existing.isPresent());

        ExperimentEvaluation evaluation = existing.get();
        BigDecimal newScore = new BigDecimal("87.25");
        evaluation.setScore(newScore);

        // When
        ExperimentEvaluation updated = evaluationRepository.save(evaluation);

        // Then
        assertNotNull(updated);
        assertEquals(newScore, updated.getScore());
    }
}
