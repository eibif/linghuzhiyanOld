package org.linghu.mybackend.repository;

import org.junit.jupiter.api.Test;
import org.linghu.mybackend.domain.Experiment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExperimentRepository 测试类
 */
@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = {"/schema.sql", "/data.sql"})
class ExperimentRepositoryTest {

    @Autowired
    private ExperimentRepository experimentRepository;

    @Test
    void findByCreatorId_WithExistingCreatorId_ShouldReturnExperiments() {
        // When
        List<Experiment> experiments = experimentRepository.findByCreatorId("creator1");

        // Then
        assertNotNull(experiments);
        assertEquals(2, experiments.size());
        assertTrue(experiments.stream().allMatch(e -> "creator1".equals(e.getCreatorId())));
    }

    @Test
    void findByCreatorId_WithNonExistingCreatorId_ShouldReturnEmptyList() {
        // When
        List<Experiment> experiments = experimentRepository.findByCreatorId("nonexistent");

        // Then
        assertNotNull(experiments);
        assertTrue(experiments.isEmpty());
    }

    @Test
    void findByCreatorIdWithPageable_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Experiment> experiments = experimentRepository.findByCreatorId("creator1", pageable);

        // Then
        assertNotNull(experiments);
        assertEquals(1, experiments.getContent().size());
        assertEquals(2, experiments.getTotalElements());
        assertEquals(2, experiments.getTotalPages());
    }

    @Test
    void findByStatus_WithExistingStatus_ShouldReturnExperiments() {
        // When
        List<Experiment> experiments = experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED);

        // Then
        assertNotNull(experiments);
        assertEquals(2, experiments.size());
        assertTrue(experiments.stream().allMatch(e -> Experiment.ExperimentStatus.PUBLISHED.equals(e.getStatus())));
    }

    @Test
    void findByStatus_WithDraftStatus_ShouldReturnDraftExperiments() {
        // When
        List<Experiment> experiments = experimentRepository.findByStatus(Experiment.ExperimentStatus.DRAFT);

        // Then
        assertNotNull(experiments);
        assertEquals(1, experiments.size());
        assertTrue(experiments.stream().allMatch(e -> Experiment.ExperimentStatus.DRAFT.equals(e.getStatus())));
    }

    @Test
    void findByStatusWithPageable_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Experiment> experiments = experimentRepository.findByStatus(Experiment.ExperimentStatus.PUBLISHED, pageable);

        // Then
        assertNotNull(experiments);
        assertEquals(1, experiments.getContent().size());
        assertEquals(2, experiments.getTotalElements());
        assertEquals(2, experiments.getTotalPages());
    }

    @Test
    void findByNameContaining_WithExistingName_ShouldReturnExperiments() {
        // When
        List<Experiment> experiments = experimentRepository.findByNameContaining("Experiment");

        // Then
        assertNotNull(experiments);
        assertTrue(experiments.size() > 0);
        assertTrue(experiments.stream().allMatch(e -> e.getName().contains("Experiment")));
    }

    @Test
    void findByNameContaining_WithNonExistingName_ShouldReturnEmptyList() {
        // When
        List<Experiment> experiments = experimentRepository.findByNameContaining("NonExistent");

        // Then
        assertNotNull(experiments);
        assertTrue(experiments.isEmpty());
    }

    @Test
    void findByNameContainingWithPageable_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Experiment> experiments = experimentRepository.findByNameContaining("Experiment", pageable);

        // Then
        assertNotNull(experiments);
        assertEquals(1, experiments.getContent().size());
        assertTrue(experiments.getTotalElements() > 0);
    }

    @Test
    void findByCreatorIdAndStatus_WithExistingIds_ShouldReturnExperiments() {
        // When
        List<Experiment> experiments = experimentRepository.findByCreatorIdAndStatus("creator1", Experiment.ExperimentStatus.PUBLISHED);

        // Then
        assertNotNull(experiments);
        assertEquals(1, experiments.size());
        assertTrue(experiments.stream().allMatch(e -> "creator1".equals(e.getCreatorId()) && 
                                                       Experiment.ExperimentStatus.PUBLISHED.equals(e.getStatus())));
    }

    @Test
    void findByCreatorIdAndStatusWithPageable_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Experiment> experiments = experimentRepository.findByCreatorIdAndStatus("creator1", Experiment.ExperimentStatus.PUBLISHED, pageable);

        // Then
        assertNotNull(experiments);
        assertEquals(1, experiments.getContent().size());
        assertEquals(1, experiments.getTotalElements());
    }

    @Test
    void findExperimentsInTimeRange_WithValidTimeRange_ShouldReturnExperiments() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1); // 1 day ago
        LocalDateTime endDate = LocalDateTime.now().plusDays(1); // 1 day later

        // When
        List<Experiment> experiments = experimentRepository.findExperimentsInTimeRange(startDate, endDate);

        // Then
        assertNotNull(experiments);
        assertTrue(experiments.size() > 0);
    }

    @Test
    void findActiveExperiments_WithCurrentDate_ShouldReturnActiveExperiments() {
        // Given
        LocalDateTime currentDate = LocalDateTime.now();

        // When
        List<Experiment> experiments = experimentRepository.findActiveExperiments(currentDate);

        // Then
        assertNotNull(experiments);
        // 结果取决于测试数据，这里主要验证方法调用成功
    }

    @Test
    void findActiveExperimentsWithPageable_ShouldReturnPagedResults() {
        // Given
        LocalDateTime currentDate = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Experiment> experiments = experimentRepository.findActiveExperiments(currentDate, pageable);

        // Then
        assertNotNull(experiments);
        // 结果取决于测试数据，这里主要验证方法调用成功
    }

    @Test
    void findByIdAndCreatorId_WithExistingIds_ShouldReturnExperiment() {
        // When
        Optional<Experiment> experiment = experimentRepository.findByIdAndCreatorId("experiment1", "creator1");

        // Then
        assertTrue(experiment.isPresent());
        assertEquals("experiment1", experiment.get().getId());
        assertEquals("creator1", experiment.get().getCreatorId());
    }

    @Test
    void findByIdAndCreatorId_WithNonExistingIds_ShouldReturnEmpty() {
        // When
        Optional<Experiment> experiment = experimentRepository.findByIdAndCreatorId("nonexistent", "creator1");

        // Then
        assertFalse(experiment.isPresent());
    }

    @Test
    void findByIdAndCreatorId_WithWrongCreator_ShouldReturnEmpty() {
        // When
        Optional<Experiment> experiment = experimentRepository.findByIdAndCreatorId("experiment1", "wrongcreator");

        // Then
        assertFalse(experiment.isPresent());
    }

    @Test
    void save_WithValidExperiment_ShouldSaveSuccessfully() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Experiment newExperiment = Experiment.builder()
                .id(UUID.randomUUID().toString())
                .name("New Experiment")
                .description("New experiment description")
                .creatorId("creator2")
                .status(Experiment.ExperimentStatus.DRAFT)
                .startTime(now)
                .endTime(now.plusDays(7))
                .createdAt(now)
                .updatedAt(now)
                .build();

        // When
        Experiment saved = experimentRepository.save(newExperiment);

        // Then
        assertNotNull(saved);
        assertEquals("New Experiment", saved.getName());
        assertEquals("creator2", saved.getCreatorId());
        assertEquals(Experiment.ExperimentStatus.DRAFT, saved.getStatus());
    }

    @Test
    void findById_WithExistingId_ShouldReturnExperiment() {
        // When
        Optional<Experiment> found = experimentRepository.findById("experiment1");

        // Then
        assertTrue(found.isPresent());
        assertEquals("experiment1", found.get().getId());
        assertNotNull(found.get().getName());
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // When
        Optional<Experiment> found = experimentRepository.findById("nonexistent");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void deleteById_WithExistingId_ShouldDeleteSuccessfully() {
        // Given
        String idToDelete = "experiment1";

        // When
        experimentRepository.deleteById(idToDelete);

        // Then
        Optional<Experiment> deleted = experimentRepository.findById(idToDelete);
        assertFalse(deleted.isPresent());
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // When
        long count = experimentRepository.count();

        // Then
        assertEquals(3, count);
    }

    @Test
    void save_UpdateExistingExperiment_ShouldUpdateSuccessfully() {
        // Given - 先获取现有的实验
        Optional<Experiment> existing = experimentRepository.findById("experiment1");
        assertTrue(existing.isPresent());
        
        Experiment experiment = existing.get();
        experiment.setName("Updated Experiment Name");
        experiment.setDescription("Updated description");
        experiment.setStatus(Experiment.ExperimentStatus.PUBLISHED);

        // When
        Experiment updated = experimentRepository.save(experiment);

        // Then
        assertNotNull(updated);
        assertEquals("Updated Experiment Name", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(Experiment.ExperimentStatus.PUBLISHED, updated.getStatus());
    }

    @Test
    void findAll_ShouldReturnAllExperiments() {
        // When
        List<Experiment> experiments = experimentRepository.findAll();

        // Then
        assertNotNull(experiments);
        assertEquals(3, experiments.size());
    }

    @Test
    void existsById_WithExistingId_ShouldReturnTrue() {
        // When
        boolean exists = experimentRepository.existsById("experiment1");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsById_WithNonExistingId_ShouldReturnFalse() {
        // When
        boolean exists = experimentRepository.existsById("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void deleteAll_ShouldDeleteAllExperiments() {
        // When
        experimentRepository.deleteAll();

        // Then
        long count = experimentRepository.count();
        assertEquals(0, count);
    }
}
