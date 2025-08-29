package org.linghu.mybackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.domain.Experiment;
import org.linghu.mybackend.domain.User;
import org.linghu.mybackend.dto.ExperimentDTO;
import org.linghu.mybackend.dto.ExperimentRequestDTO;
import org.linghu.mybackend.repository.ExperimentRepository;
import org.linghu.mybackend.repository.ExperimentTaskRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExperimentServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ExperimentServiceImplTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ExperimentTaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExperimentServiceImpl experimentService;

    private User testUser;
    private Experiment testExperiment;
    private ExperimentRequestDTO testExperimentRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        LocalDateTime now = LocalDateTime.now();
        testExperiment = Experiment.builder()
                .id("experiment1")
                .name("Test Experiment")
                .description("Test Description")
                .status(Experiment.ExperimentStatus.DRAFT)
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(7))
                .creatorId("user1")
                .createdAt(now)
                .updatedAt(now)
                .build();

        testExperimentRequest = ExperimentRequestDTO.builder()
                .name("Test Experiment")
                .description("Test Description")
                .status(Experiment.ExperimentStatus.DRAFT)
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(7))
                .build();
    }

    @Test
    void createExperiment_WithValidData_ShouldCreateSuccessfully() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(experimentRepository.save(any(Experiment.class))).thenReturn(testExperiment);

        // When
        ExperimentDTO result = experimentService.createExperiment(testExperimentRequest, "testuser");

        // Then
        assertNotNull(result);
        assertEquals("experiment1", result.getId());
        assertEquals("Test Experiment", result.getName());
        assertEquals("Test Description", result.getDescription());
        assertEquals("user1", result.getCreator_Id());
        assertEquals(Experiment.ExperimentStatus.DRAFT, result.getStatus());
        verify(experimentRepository).save(any(Experiment.class));
    }

    @Test
    void createExperiment_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            experimentService.createExperiment(testExperimentRequest, "nonexistent");
        });
        assertEquals("用户不存在", exception.getMessage());
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void getAllExperiments_WithValidPageParams_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Experiment> experimentPage = new PageImpl<>(List.of(testExperiment));
        when(experimentRepository.findAll(pageable)).thenReturn(experimentPage);

        // When
        Page<ExperimentDTO> result = experimentService.getAllExperiments(1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("experiment1", result.getContent().get(0).getId());
        assertEquals("Test Experiment", result.getContent().get(0).getName());
    }

    @Test
    void getAllExperiments_WithInvalidPageParams_ShouldThrowException() {
        // When & Then - 测试负页数参数
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            experimentService.getAllExperiments(-1, 10);
        });
        assertEquals("Page index must not be less than zero", exception1.getMessage());

        // When & Then - 测试零或负页面大小参数
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            experimentService.getAllExperiments(0, 0);
        });
        assertEquals("Page index must not be less than zero", exception2.getMessage());

        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () -> {
            experimentService.getAllExperiments(0, -5);
        });
        assertEquals("Page index must not be less than zero", exception3.getMessage());
    }

    @Test
    void getExperimentById_WithExistingId_ShouldReturnExperiment() {
        // Given
        when(experimentRepository.findById("experiment1")).thenReturn(Optional.of(testExperiment));

        // When
        ExperimentDTO result = experimentService.getExperimentById("experiment1");

        // Then
        assertNotNull(result);
        assertEquals("experiment1", result.getId());
        assertEquals("Test Experiment", result.getName());
        assertEquals("Test Description", result.getDescription());
    }

    @Test
    void getExperimentById_WithNonExistingId_ShouldThrowException() {
        // Given
        when(experimentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            experimentService.getExperimentById("nonexistent");
        });
        assertEquals("实验不存在", exception.getMessage());
    }

    @Test
    void updateExperiment_WithValidDataAndPermission_ShouldUpdateSuccessfully() {
        // Given
        when(experimentRepository.findById("experiment1")).thenReturn(Optional.of(testExperiment));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(experimentRepository.save(any(Experiment.class))).thenReturn(testExperiment);

        ExperimentRequestDTO updateRequest = ExperimentRequestDTO.builder()
                .name("Updated Experiment")
                .description("Updated Description")
                .status(Experiment.ExperimentStatus.PUBLISHED)
                .startTime(testExperimentRequest.getStartTime())
                .endTime(testExperimentRequest.getEndTime())
                .build();

        // When
        ExperimentDTO result = experimentService.updateExperiment("experiment1", updateRequest, "testuser");

        // Then
        assertNotNull(result);
        assertEquals("experiment1", result.getId());
        verify(experimentRepository).save(any(Experiment.class));
    }

    @Test
    void updateExperiment_WithNonExistingExperiment_ShouldThrowException() {
        // Given
        when(experimentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            experimentService.updateExperiment("nonexistent", testExperimentRequest, "testuser");
        });
        assertEquals("实验不存在", exception.getMessage());
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void updateExperiment_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(experimentRepository.findById("experiment1")).thenReturn(Optional.of(testExperiment));
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            experimentService.updateExperiment("experiment1", testExperimentRequest, "nonexistent");
        });
        assertEquals("用户不存在", exception.getMessage());
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void updateExperiment_WithoutPermission_ShouldThrowException() {
        // Given
        User anotherUser = new User();
        anotherUser.setId("user2");
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");

        when(experimentRepository.findById("experiment1")).thenReturn(Optional.of(testExperiment));
        when(userRepository.findByUsername("anotheruser")).thenReturn(Optional.of(anotherUser));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            experimentService.updateExperiment("experiment1", testExperimentRequest, "anotheruser");
        });
        assertEquals("无权限更新此实验", exception.getMessage());
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void deleteExperiment_WithExistingId_ShouldDeleteSuccessfully() {
        // Given
        when(experimentRepository.existsById("experiment1")).thenReturn(true);

        // When
        experimentService.deleteExperiment("experiment1");

        // Then
        verify(experimentRepository).deleteById("experiment1");
    }

    @Test
    void deleteExperiment_WithNonExistingId_ShouldThrowException() {
        // Given
        when(experimentRepository.existsById("nonexistent")).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            experimentService.deleteExperiment("nonexistent");
        });
        assertEquals("实验不存在", exception.getMessage());
        verify(experimentRepository, never()).deleteById(any());
    }

    @Test
    void publishExperiment_WithExistingExperiment_ShouldPublishSuccessfully() {
        // Given
        when(experimentRepository.findById("experiment1")).thenReturn(Optional.of(testExperiment));
        
        Experiment publishedExperiment = Experiment.builder()
                .id("experiment1")
                .name("Test Experiment")
                .description("Test Description")
                .status(Experiment.ExperimentStatus.PUBLISHED)
                .startTime(testExperiment.getStartTime())
                .endTime(testExperiment.getEndTime())
                .creatorId("user1")
                .createdAt(testExperiment.getCreatedAt())
                .updatedAt(testExperiment.getUpdatedAt())
                .build();
                
        when(experimentRepository.save(any(Experiment.class))).thenReturn(publishedExperiment);

        // When
        ExperimentDTO result = experimentService.publishExperiment("experiment1");

        // Then
        assertNotNull(result);
        assertEquals("experiment1", result.getId());
        assertEquals(Experiment.ExperimentStatus.PUBLISHED, result.getStatus());
        verify(experimentRepository).save(any(Experiment.class));
    }

    @Test
    void publishExperiment_WithNonExistingExperiment_ShouldThrowException() {
        // Given
        when(experimentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            experimentService.publishExperiment("nonexistent");
        });
        assertEquals("实验不存在", exception.getMessage());
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void unpublishExperiment_WithExistingExperiment_ShouldUnpublishSuccessfully() {
        // Given
        Experiment publishedExperiment = Experiment.builder()
                .id("experiment1")
                .name("Test Experiment")
                .description("Test Description")
                .status(Experiment.ExperimentStatus.PUBLISHED)
                .startTime(testExperiment.getStartTime())
                .endTime(testExperiment.getEndTime())
                .creatorId("user1")
                .createdAt(testExperiment.getCreatedAt())
                .updatedAt(testExperiment.getUpdatedAt())
                .build();

        when(experimentRepository.findById("experiment1")).thenReturn(Optional.of(publishedExperiment));
        
        Experiment unpublishedExperiment = Experiment.builder()
                .id("experiment1")
                .name("Test Experiment")
                .description("Test Description")
                .status(Experiment.ExperimentStatus.DRAFT)
                .startTime(testExperiment.getStartTime())
                .endTime(testExperiment.getEndTime())
                .creatorId("user1")
                .createdAt(testExperiment.getCreatedAt())
                .updatedAt(testExperiment.getUpdatedAt())
                .build();
                
        when(experimentRepository.save(any(Experiment.class))).thenReturn(unpublishedExperiment);

        // When
        ExperimentDTO result = experimentService.unpublishExperiment("experiment1");

        // Then
        assertNotNull(result);
        assertEquals("experiment1", result.getId());
        assertEquals(Experiment.ExperimentStatus.DRAFT, result.getStatus());
        verify(experimentRepository).save(any(Experiment.class));
    }

    @Test
    void unpublishExperiment_WithNonExistingExperiment_ShouldThrowException() {
        // Given
        when(experimentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            experimentService.unpublishExperiment("nonexistent");
        });
        assertEquals("实验不存在", exception.getMessage());
        verify(experimentRepository, never()).save(any(Experiment.class));
    }
}
