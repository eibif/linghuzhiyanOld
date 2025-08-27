package org.linghu.mybackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.linghu.mybackend.LinHuZhiYanApplication;
import org.linghu.mybackend.dto.ResourceDTO;
import org.linghu.mybackend.dto.ResourceRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest(classes = LinHuZhiYanApplication.class)
public class ResourceServiceTest {

    @Autowired
    private ResourceService resourceService;

    @Test
    public void testUploadResourceWithPrefixes() {
        // 测试通用资源上传
        MultipartFile file = new MockMultipartFile(
                "test.txt",
                "test.txt",
                "text/plain",
                "This is a test file".getBytes()
        );

        ResourceRequestDTO requestDTO = ResourceRequestDTO.builder()
                .resourceType("DOCUMENT")
                .description("Test document")
                .build();

        ResourceDTO resourceDTO = resourceService.uploadResource(file, requestDTO);
        
        assertNotNull(resourceDTO);
        assertEquals("DOCUMENT", resourceDTO.getResourceType());
        assertTrue(resourceDTO.getResourcePath().contains("resources/document/"));
        
        // 测试实验资源上传
        String experimentId = "test-experiment-id";
        requestDTO = ResourceRequestDTO.builder()
                .experimentId(experimentId)
                .resourceType("DOCUMENT")
                .description("Test experiment document")
                .build();

        resourceDTO = resourceService.uploadResource(file, requestDTO);
        
        assertNotNull(resourceDTO);
        assertEquals("DOCUMENT", resourceDTO.getResourceType());
        assertTrue(resourceDTO.getResourcePath().contains("experiments/" + experimentId));
        
        // 测试学生提交上传
        String studentId = "test-student-id";
        String taskId = "test-task-id";
        
        resourceDTO = resourceService.uploadStudentSubmission(file, studentId, experimentId, taskId, requestDTO);
        
        assertNotNull(resourceDTO);
        assertEquals("SUBMISSION", resourceDTO.getResourceType());
        assertTrue(resourceDTO.getResourcePath().contains("submissions/" + studentId + "/" + experimentId));
    }
}
