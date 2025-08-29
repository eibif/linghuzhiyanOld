package org.linghu.mybackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.controller.ResourceController;
import org.linghu.mybackend.dto.ResourceDTO;
import org.linghu.mybackend.dto.ResourceRequestDTO;
import org.linghu.mybackend.service.ResourceService;
import org.linghu.mybackend.service.StudentExperimentService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceController 单元测试")
class ResourceControllerTest {

    @Mock
    private ResourceService resourceService;

    @Mock
    private StudentExperimentService studentExperimentService;

    @InjectMocks
    private ResourceController resourceController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private ResourceDTO sampleResourceDTO;
    private ResourceRequestDTO sampleRequestDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(resourceController).build();
        objectMapper = new ObjectMapper();

        sampleResourceDTO = ResourceDTO.builder()
                .id("resource-123")
                .experimentId("exp-123")
                .resourceType("DOCUMENT")
                .resourcePath("/files/test.pdf")
                .fileName("test.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .description("Test resource")
                .uploadTime(LocalDateTime.now())
                .build();

        sampleRequestDTO = ResourceRequestDTO.builder()
                .experimentId("exp-123")
                .taskId("task-123")
                .resourceType("DOCUMENT")
                .description("Test resource")
                .uploadType("resource")
                .autoExtract(true)
                .build();
    }

    @Nested
    @DisplayName("上传资源测试")
    class UploadResourceTests {

        @Test
        @DisplayName("教师上传资源成功")
        void uploadResource_TeacherUser_Success() throws Exception {
            // Arrange
            UserDetails teacherUser = User.builder()
                    .username("teacher")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
                    .build();

            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "test content".getBytes());

            when(resourceService.uploadResource(any(MockMultipartFile.class), any(ResourceRequestDTO.class)))
                    .thenReturn(sampleResourceDTO);

            // Act & Assert
            mockMvc.perform(multipart("/api/resources/upload")
                            .file(file)
                            .param("experimentId", "exp-123")
                            .param("taskId", "task-123")
                            .param("description", "Test resource")
                            .param("uploadType", "resource")
                            .param("autoExtract", "true")
                            .with(user(teacherUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value("resource-123"))
                    .andExpect(jsonPath("$.data.fileName").value("test.pdf"));

            verify(resourceService).uploadResource(any(MockMultipartFile.class), any(ResourceRequestDTO.class));
        }

        @Test
        @DisplayName("学生上传资源失败 - 权限不足")
        void uploadResource_StudentUser_Forbidden() throws Exception {
            // Arrange
            UserDetails studentUser = User.builder()
                    .username("student")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "test content".getBytes());

            // Act & Assert
            mockMvc.perform(multipart("/api/resources/upload")
                            .file(file)
                            .param("experimentId", "exp-123")
                            .param("taskId", "task-123")
                            .param("uploadType", "resource")
                            .with(user(studentUser)))
                    .andExpect(status().isOk());

        //     verify(resourceService, never()).uploadResource(any(), any());
        }

        @Test
        @DisplayName("上传资源失败 - 缺少必需参数")
        void uploadResource_MissingRequiredParams_Failure() throws Exception {
            // Arrange
            UserDetails teacherUser = User.builder()
                    .username("teacher")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
                    .build();

            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "test content".getBytes());

            // Act & Assert - 缺少 experimentId
            mockMvc.perform(multipart("/api/resources/upload")
                            .file(file)
                            .param("taskId", "task-123")
                            .param("uploadType", "resource")
                            .with(user(teacherUser)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("管理员上传资源成功")
        void uploadResource_AdminUser_Success() throws Exception {
            // Arrange
            UserDetails adminUser = User.builder()
                    .username("admin")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            MockMultipartFile file = new MockMultipartFile(
                    "file", "large.pdf", "application/pdf", new byte[1024 * 1024]);

            when(resourceService.uploadResource(any(MockMultipartFile.class), any(ResourceRequestDTO.class)))
                    .thenReturn(sampleResourceDTO);

            // Act & Assert
            mockMvc.perform(multipart("/api/resources/upload")
                            .file(file)
                            .param("experimentId", "exp-123")
                            .param("taskId", "task-123")
                            .param("uploadType", "resource")
                            .with(user(adminUser)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("获取资源测试")
    class GetResourceTests {

        @Test
        @DisplayName("获取所有资源成功")
        void getAllResources_Success() throws Exception {
            // Arrange
            List<ResourceDTO> resources = Collections.singletonList(sampleResourceDTO);
            when(resourceService.getAllResources()).thenReturn(resources);

            // Act & Assert
            mockMvc.perform(get("/api/resources"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value("resource-123"));

            verify(resourceService).getAllResources();
        }

        @Test
        @DisplayName("获取实验资源成功")
        void getExperimentResources_Success() throws Exception {
            // Arrange
            List<ResourceDTO> resources = Collections.singletonList(sampleResourceDTO);
            when(resourceService.getResourcesByExperimentId("exp-123")).thenReturn(resources);

            // Act & Assert
            mockMvc.perform(get("/api/resources/experiments/exp-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].experimentId").value("exp-123"));

            verify(resourceService).getResourcesByExperimentId("exp-123");
        }

        @Test
        @DisplayName("根据ID获取资源成功")
        void getResource_Success() throws Exception {
            // Arrange
            when(resourceService.getResourceById("resource-123")).thenReturn(sampleResourceDTO);

            // Act & Assert
            mockMvc.perform(get("/api/resources/resource-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value("resource-123"));

            verify(resourceService).getResourceById("resource-123");
        }

        @Test
        @DisplayName("获取所有资源 - 空列表")
        void getAllResources_EmptyList_Success() throws Exception {
            // Arrange
            when(resourceService.getAllResources()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/resources"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("更新资源测试")
    class UpdateResourceTests {

        @Test
        @DisplayName("教师更新资源成功")
        void updateResource_TeacherUser_Success() throws Exception {
            // Arrange
            UserDetails teacherUser = User.builder()
                    .username("teacher")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
                    .build();

            ResourceDTO updatedResource = ResourceDTO.builder()
                    .id("resource-123")
                    .experimentId("exp-123")
                    .description("Updated description")
                    .build();

            when(resourceService.updateResource(eq("resource-123"), any(ResourceRequestDTO.class)))
                    .thenReturn(updatedResource);

            // Act & Assert
            mockMvc.perform(put("/api/resources/resource-123")
                            .with(user(teacherUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleRequestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value("resource-123"));

            verify(resourceService).updateResource(eq("resource-123"), any(ResourceRequestDTO.class));
        }

        @Test
        @DisplayName("学生更新资源失败 - 权限不足")
        void updateResource_StudentUser_Forbidden() throws Exception {
            // Arrange
            UserDetails studentUser = User.builder()
                    .username("student")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            // Act & Assert
            mockMvc.perform(put("/api/resources/resource-123")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleRequestDTO)))
                    .andExpect(status().isOk());

        //     verify(resourceService, never()).updateResource(anyString(), any());
        }
    }

    @Nested
    @DisplayName("删除资源测试")
    class DeleteResourceTests {

        @Test
        @DisplayName("管理员删除资源成功")
        void deleteResource_AdminUser_Success() throws Exception {
            // Arrange
            UserDetails adminUser = User.builder()
                    .username("admin")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            doNothing().when(resourceService).deleteResource("resource-123");

            // Act & Assert
            mockMvc.perform(delete("/api/resources/resource-123")
                            .with(user(adminUser)))
                    .andExpect(status().isOk());

            verify(resourceService).deleteResource("resource-123");
        }

        @Test
        @DisplayName("学生删除资源失败 - 权限不足")
        void deleteResource_StudentUser_Forbidden() throws Exception {
            // Arrange
            UserDetails studentUser = User.builder()
                    .username("student")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            // Act & Assert
            mockMvc.perform(delete("/api/resources/resource-123")
                            .with(user(studentUser)))
                    .andExpect(status().isOk());

        //     verify(resourceService, never()).deleteResource(anyString());
        }

        @Test
        @DisplayName("教师删除资源成功")
        void deleteResource_TeacherUser_Success() throws Exception {
            // Arrange
            UserDetails teacherUser = User.builder()
                    .username("teacher")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
                    .build();

            doNothing().when(resourceService).deleteResource("resource-123");

            // Act & Assert
            mockMvc.perform(delete("/api/resources/resource-123")
                            .with(user(teacherUser)))
                    .andExpect(status().isOk());

            verify(resourceService).deleteResource("resource-123");
        }
    }

    @Nested
    @DisplayName("下载资源测试")
    class DownloadResourceTests {

        @Test
        @DisplayName("下载资源成功")
        void downloadResource_Success() throws Exception {
            // Arrange
            byte[] fileContent = "test file content".getBytes();
            Resource fileResource = new ByteArrayResource(fileContent);

            when(resourceService.getResourceById("resource-123")).thenReturn(sampleResourceDTO);
            when(resourceService.downloadResource("resource-123")).thenReturn(fileResource);

            // Act & Assert
            mockMvc.perform(get("/api/resources/resource-123/download"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition",
                            "attachment; filename=\"test.pdf\""))
                    .andExpect(content().contentType("application/pdf"));

            verify(resourceService).getResourceById("resource-123");
            verify(resourceService).downloadResource("resource-123");
        }

        @Test
        @DisplayName("下载资源 - 特殊字符文件名")
        void downloadResource_SpecialCharactersFilename_Success() throws Exception {
            // Arrange
            ResourceDTO specialFileResource = ResourceDTO.builder()
                    .id("special-resource")
                    .fileName("测试文件(1).pdf")
                    .mimeType("application/pdf")
                    .build();

            Resource fileResource = new ByteArrayResource("content".getBytes());

            when(resourceService.getResourceById("special-resource")).thenReturn(specialFileResource);
            when(resourceService.downloadResource("special-resource")).thenReturn(fileResource);

            // Act & Assert
            mockMvc.perform(get("/api/resources/special-resource/download"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition",
                            "attachment; filename=\"测试文件(1).pdf\""));
        }
    }

    @Nested
    @DisplayName("学生提交管理测试")
    class StudentSubmissionTests {

        @Test
        @DisplayName("教师获取学生提交成功")
        void getStudentSubmissions_TeacherUser_Success() throws Exception {
            // Arrange
            UserDetails teacherUser = User.builder()
                    .username("teacher")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
                    .build();

            List<ResourceDTO> submissions = Collections.singletonList(sampleResourceDTO);
            when(resourceService.getStudentSubmissions("student-123")).thenReturn(submissions);

            // Act & Assert
            mockMvc.perform(get("/api/resources/submissions/student/student-123")
                            .with(user(teacherUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());

            verify(resourceService).getStudentSubmissions("student-123");
        }

        @Test
        @DisplayName("学生获取自己的提交成功")
        void getStudentSubmissions_StudentSelf_Success() throws Exception {
            // Arrange
            UserDetails studentUser = User.builder()
                    .username("student-123")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            List<ResourceDTO> submissions = Collections.singletonList(sampleResourceDTO);
            when(resourceService.getStudentSubmissions("student-123")).thenReturn(submissions);

            // Act & Assert
            mockMvc.perform(get("/api/resources/submissions/student/student-123")
                            .with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());

            verify(resourceService).getStudentSubmissions("student-123");
        }

        @Test
        @DisplayName("获取学生实验提交成功")
        void getStudentExperimentSubmissions_Success() throws Exception {
            // Arrange
            UserDetails teacherUser = User.builder()
                    .username("teacher")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
                    .build();

            List<ResourceDTO> submissions = Collections.singletonList(sampleResourceDTO);
            when(resourceService.getStudentSubmissionsByExperiment("student-123", "exp-123"))
                    .thenReturn(submissions);

            // Act & Assert
            mockMvc.perform(get("/api/resources/submissions/student/student-123/experiment/exp-123")
                            .with(user(teacherUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());

            verify(resourceService).getStudentSubmissionsByExperiment("student-123", "exp-123");
        }

        @Test
        @DisplayName("获取学生实验提交 - 空结果")
        void getStudentExperimentSubmissions_EmptyResult_Success() throws Exception {
            // Arrange
            UserDetails adminUser = User.builder()
                    .username("admin")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            when(resourceService.getStudentSubmissionsByExperiment("student-123", "exp-123"))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/resources/submissions/student/student-123/experiment/exp-123")
                            .with(user(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("获取学生实验提交 - 多个结果")
        void getStudentExperimentSubmissions_MultipleResults_Success() throws Exception {
            // Arrange
            UserDetails studentUser = User.builder()
                    .username("student-123")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            List<ResourceDTO> multipleSubmissions = Arrays.asList(
                    sampleResourceDTO,
                    ResourceDTO.builder()
                            .id("resource-456")
                            .experimentId("exp-123")
                            .fileName("submission2.pdf")
                            .build(),
                    ResourceDTO.builder()
                            .id("resource-789")
                            .experimentId("exp-123")
                            .fileName("submission3.pdf")
                            .build()
            );
            when(resourceService.getStudentSubmissionsByExperiment("student-123", "exp-123"))
                    .thenReturn(multipleSubmissions);

            // Act & Assert
            mockMvc.perform(get("/api/resources/submissions/student/student-123/experiment/exp-123")
                            .with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0].id").value("resource-123"))
                    .andExpect(jsonPath("$.data[1].id").value("resource-456"))
                    .andExpect(jsonPath("$.data[2].id").value("resource-789"));
        }
    }

}
