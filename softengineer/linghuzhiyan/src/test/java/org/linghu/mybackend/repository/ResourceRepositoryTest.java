package org.linghu.mybackend.repository;

import org.junit.jupiter.api.Test;
import org.linghu.mybackend.domain.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResourceRepository 单元测试
 */
@DataJpaTest
@Sql(scripts = {"/schema.sql", "/data.sql"})
class ResourceRepositoryTest {

    @Autowired
    private ResourceRepository resourceRepository;

    // ==================== findByExperimentId 测试 ====================

    @Test
    void findByExperimentId_ShouldReturnResources_WhenExperimentExists() {
        // Given
        String experimentId = "exp-001";

        // When
        List<Resource> resources = resourceRepository.findByExperimentId(experimentId);

        // Then
        assertThat(resources).isNotEmpty();
        assertThat(resources).hasSize(2);
        assertThat(resources).allMatch(resource -> experimentId.equals(resource.getExperimentId()));
        assertThat(resources).extracting(Resource::getFileName)
                .containsExactlyInAnyOrder("test1.pdf", "test2.jpg");
    }

    @Test
    void findByExperimentId_ShouldReturnEmptyList_WhenExperimentNotExists() {
        // Given
        String nonExistentExperimentId = "non-existent-exp";

        // When
        List<Resource> resources = resourceRepository.findByExperimentId(nonExistentExperimentId);

        // Then
        assertThat(resources).isEmpty();
    }

    // ==================== findByResourceType 测试 ====================

    @Test
    void findByResourceType_ShouldReturnResources_WhenTypeExists() {
        // Given
        Resource.ResourceType resourceType = Resource.ResourceType.DOCUMENT;

        // When
        List<Resource> resources = resourceRepository.findByResourceType(resourceType);

        // Then
        assertThat(resources).isNotEmpty();
        assertThat(resources).hasSize(2);
        assertThat(resources).allMatch(resource -> resourceType.equals(resource.getResourceType()));
        assertThat(resources).extracting(Resource::getFileName)
                .containsExactlyInAnyOrder("test1.pdf", "public.pdf");
    }

    @Test
    void findByResourceType_ShouldReturnEmptyList_WhenTypeNotExists() {
        // Given - 使用数据中不存在的类型
        Resource.ResourceType resourceType = Resource.ResourceType.AUDIO;

        // When
        List<Resource> resources = resourceRepository.findByResourceType(resourceType);

        // Then
        assertThat(resources).isEmpty();
    }

    // ==================== findByExperimentIdAndResourceType 测试 ====================

    @Test
    void findByExperimentIdAndResourceType_ShouldReturnResources_WhenBothMatch() {
        // Given
        String experimentId = "exp-001";
        Resource.ResourceType resourceType = Resource.ResourceType.DOCUMENT;

        // When
        List<Resource> resources = resourceRepository.findByExperimentIdAndResourceType(experimentId, resourceType);

        // Then
        assertThat(resources).isNotEmpty();
        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getExperimentId()).isEqualTo(experimentId);
        assertThat(resources.get(0).getResourceType()).isEqualTo(resourceType);
        assertThat(resources.get(0).getFileName()).isEqualTo("test1.pdf");
    }

    @Test
    void findByExperimentIdAndResourceType_ShouldReturnEmptyList_WhenNoMatch() {
        // Given
        String experimentId = "exp-001";
        Resource.ResourceType resourceType = Resource.ResourceType.VIDEO; // exp-001没有VIDEO类型

        // When
        List<Resource> resources = resourceRepository.findByExperimentIdAndResourceType(experimentId, resourceType);

        // Then
        assertThat(resources).isEmpty();
    }

    // ==================== findByFileNameContaining 测试 ====================

    @Test
    void findByFileNameContaining_ShouldReturnPagedResults_WhenFilesMatch() {
        // Given
        String fileName = "test";
        PageRequest pageRequest = PageRequest.of(0, 10);

        // When
        Page<Resource> resourcePage = resourceRepository.findByFileNameContaining(fileName, pageRequest);

        // Then
        assertThat(resourcePage.getContent()).isNotEmpty();
        assertThat(resourcePage.getContent()).hasSize(4); // test1.pdf, test2.jpg, test3.mp4, test4.java
        assertThat(resourcePage.getTotalElements()).isEqualTo(4);
        assertThat(resourcePage.getContent()).allMatch(resource -> 
                resource.getFileName().toLowerCase().contains(fileName.toLowerCase()));
    }

    @Test
    void findByFileNameContaining_ShouldReturnEmptyPage_WhenNoFilesMatch() {
        // Given
        String fileName = "nonexistent";
        PageRequest pageRequest = PageRequest.of(0, 10);

        // When
        Page<Resource> resourcePage = resourceRepository.findByFileNameContaining(fileName, pageRequest);

        // Then
        assertThat(resourcePage.getContent()).isEmpty();
        assertThat(resourcePage.getTotalElements()).isZero();
    }

    // ==================== findByMimeType 测试 ====================

    @Test
    void findByMimeType_ShouldReturnResources_WhenMimeTypeExists() {
        // Given
        String mimeType = "application/pdf";

        // When
        List<Resource> resources = resourceRepository.findByMimeType(mimeType);

        // Then
        assertThat(resources).isNotEmpty();
        assertThat(resources).hasSize(2);
        assertThat(resources).allMatch(resource -> mimeType.equals(resource.getMimeType()));
        assertThat(resources).extracting(Resource::getFileName)
                .containsExactlyInAnyOrder("test1.pdf", "public.pdf");
    }

    @Test
    void findByMimeType_ShouldReturnEmptyList_WhenMimeTypeNotExists() {
        // Given
        String mimeType = "application/unknown";

        // When
        List<Resource> resources = resourceRepository.findByMimeType(mimeType);

        // Then
        assertThat(resources).isEmpty();
    }

    // ==================== existsByExperimentId 测试 ====================

    @Test
    void existsByExperimentId_ShouldReturnTrue_WhenExperimentHasResources() {
        // Given
        String experimentId = "exp-001";

        // When
        boolean exists = resourceRepository.existsByExperimentId(experimentId);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByExperimentId_ShouldReturnFalse_WhenExperimentHasNoResources() {
        // Given
        String experimentId = "non-existent-exp";

        // When
        boolean exists = resourceRepository.existsByExperimentId(experimentId);

        // Then
        assertThat(exists).isFalse();
    }

    // ==================== findByResourcePath 测试 ====================

    @Test
    void findByResourcePath_ShouldReturnResources_WhenPathExists() {
        // Given
        String resourcePath = "/path/to/test1.pdf";

        // When
        List<Resource> resources = resourceRepository.findByResourcePath(resourcePath);

        // Then
        assertThat(resources).isNotEmpty();
        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getResourcePath()).isEqualTo(resourcePath);
        assertThat(resources.get(0).getFileName()).isEqualTo("test1.pdf");
    }

    @Test
    void findByResourcePath_ShouldReturnEmptyList_WhenPathNotExists() {
        // Given
        String resourcePath = "/path/to/nonexistent.pdf";

        // When
        List<Resource> resources = resourceRepository.findByResourcePath(resourcePath);

        // Then
        assertThat(resources).isEmpty();
    }

    // ==================== JPA Repository 基本方法测试 ====================

    @Test
    void findById_ShouldReturnResource_WhenIdExists() {
        // Given
        String resourceId = "test-resource-1";

        // When
        Optional<Resource> resourceOpt = resourceRepository.findById(resourceId);

        // Then
        assertThat(resourceOpt).isPresent();
        assertThat(resourceOpt.get().getId()).isEqualTo(resourceId);
        assertThat(resourceOpt.get().getFileName()).isEqualTo("test1.pdf");
        assertThat(resourceOpt.get().getExperimentId()).isEqualTo("exp-001");
    }

    @Test
    void findById_ShouldReturnEmpty_WhenIdNotExists() {
        // Given
        String resourceId = "non-existent-id";

        // When
        Optional<Resource> resourceOpt = resourceRepository.findById(resourceId);

        // Then
        assertThat(resourceOpt).isEmpty();
    }

    @Test
    void findAll_ShouldReturnAllResources() {
        // When
        List<Resource> resources = resourceRepository.findAll();

        // Then
        assertThat(resources).hasSize(6); // 根据data.sql中的数据
        assertThat(resources).extracting(Resource::getId)
                .containsExactlyInAnyOrder(
                        "test-resource-1", "test-resource-2", "test-resource-3",
                        "test-resource-4", "test-resource-5", "test-resource-6");
    }

    @Test
    void save_ShouldPersistResource_WhenValidResource() {
        // Given
        Resource newResource = Resource.builder()
                .id("new-resource-1")
                .experimentId("exp-004")
                .resourceType(Resource.ResourceType.CODE)
                .resourcePath("/path/to/new.java")
                .fileName("new.java")
                .fileSize(1024L)
                .mimeType("text/plain")
                .description("新测试资源")
                .build();

        // When
        Resource savedResource = resourceRepository.save(newResource);

        // Then
        assertThat(savedResource).isNotNull();
        assertThat(savedResource.getId()).isEqualTo("new-resource-1");
        assertThat(savedResource.getUploadTime()).isNotNull(); // @PrePersist应该设置时间

        // 验证持久化
        Optional<Resource> foundResource = resourceRepository.findById("new-resource-1");
        assertThat(foundResource).isPresent();
        assertThat(foundResource.get().getFileName()).isEqualTo("new.java");
    }

    @Test
    void delete_ShouldRemoveResource_WhenResourceExists() {
        // Given
        String resourceId = "test-resource-1";
        assertThat(resourceRepository.findById(resourceId)).isPresent();

        // When
        resourceRepository.deleteById(resourceId);

        // Then
        assertThat(resourceRepository.findById(resourceId)).isEmpty();
        
        // 验证总数减少
        List<Resource> allResources = resourceRepository.findAll();
        assertThat(allResources).hasSize(5); // 原来6个，删除1个
    }

    @Test
    void delete_ShouldNotThrowException_WhenResourceNotExists() {
        // Given
        String nonExistentId = "non-existent-id";

        // When & Then - 不应该抛出异常
        resourceRepository.deleteById(nonExistentId);
        
        // 验证总数没有变化
        List<Resource> allResources = resourceRepository.findAll();
        assertThat(allResources).hasSize(6);
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // When
        long count = resourceRepository.count();

        // Then
        assertThat(count).isEqualTo(6L);
    }

    @Test
    void existsById_ShouldReturnTrue_WhenResourceExists() {
        // Given
        String resourceId = "test-resource-1";

        // When
        boolean exists = resourceRepository.existsById(resourceId);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsById_ShouldReturnFalse_WhenResourceNotExists() {
        // Given
        String resourceId = "non-existent-id";

        // When
        boolean exists = resourceRepository.existsById(resourceId);

        // Then
        assertThat(exists).isFalse();
    }
}
