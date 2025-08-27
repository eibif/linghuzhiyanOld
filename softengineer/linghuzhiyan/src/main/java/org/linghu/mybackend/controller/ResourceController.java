package org.linghu.mybackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.linghu.mybackend.dto.ResourceDTO;
import org.linghu.mybackend.dto.ResourceRequestDTO;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.service.ResourceService;
import org.linghu.mybackend.service.StudentExperimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资源管理API控制器
 */
@RestController
@RequestMapping("/api/resources")
@Tag(name = "资源管理", description = "实验和任务资源管理相关API")
public class ResourceController {

    private final ResourceService resourceService;
    private final StudentExperimentService studentExperimentService;

    @Autowired
    public ResourceController(ResourceService resourceService, StudentExperimentService studentExperimentService) {
        this.resourceService = resourceService;
        this.studentExperimentService = studentExperimentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN','ROLE_ASSISTANT')")
    @Operation(summary = "上传资源", description = "上传资源文件")
    public Result<ResourceDTO> uploadResource(
            @RequestParam("file") MultipartFile file,
            @RequestParam String experimentId,
            @RequestParam String taskId,
            @RequestParam(required = false) String description,
            @RequestParam String uploadType,
            @RequestParam(required = false, defaultValue = "true") Boolean autoExtract) {

        ResourceRequestDTO requestDTO = ResourceRequestDTO.builder()
                .experimentId(experimentId)
                .taskId(taskId)
                .description(description)
                .uploadType(uploadType)
                .autoExtract(autoExtract)
                .build();

        ResourceDTO resource = resourceService.uploadResource(file, requestDTO);
        return Result.success(resource);
    }

    @GetMapping("")
    @Operation(summary = "获取所有资源列表", description = "获取系统中的所有资源")
    public Result<List<ResourceDTO>> getAllResources() {
        List<ResourceDTO> resources = resourceService.getAllResources();
        return Result.success(resources);
    }

    @GetMapping("/experiments/{expId}")
    @Operation(summary = "获取实验资源列表", description = "获取指定实验的所有资源")
    public Result<List<ResourceDTO>> getExperimentResources(@PathVariable String expId) {
        List<ResourceDTO> resources = resourceService.getResourcesByExperimentId(expId);
        return Result.success(resources);
    }

    @GetMapping("/{resourceId}")
    @Operation(summary = "获取资源详情", description = "获取指定资源的详细信息")
    public Result<ResourceDTO> getResource(@PathVariable String resourceId) {
        ResourceDTO resource = resourceService.getResourceById(resourceId);
        return Result.success(resource);
    }

    @PutMapping("/{resourceId}")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN','ROLE_ASSISTANT')")
    @Operation(summary = "更新资源信息", description = "更新指定资源的信息，包括关联到实验或任务")
    public Result<ResourceDTO> updateResource(
            @PathVariable String resourceId,
            @RequestBody ResourceRequestDTO requestDTO) {
        ResourceDTO updatedResource = resourceService.updateResource(resourceId, requestDTO);
        return Result.success(updatedResource);
    }

    @DeleteMapping("/{resourceId}")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN','ROLE_ASSISTANT')")
    @Operation(summary = "删除资源", description = "删除指定的资源")
    public Result<Void> deleteResource(@PathVariable String resourceId) {
        resourceService.deleteResource(resourceId);
        return Result.success();
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "下载资源文件", description = "下载指定资源的文件")
    public ResponseEntity<Resource> downloadResource(@PathVariable String id) {
        ResourceDTO resourceDTO = resourceService.getResourceById(id);
        Resource fileResource = resourceService.downloadResource(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resourceDTO.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(resourceDTO.getMimeType()))
                .body(fileResource);
    }

    // @PostMapping(value = "/submissions/upload", consumes = MediaType.APPLICATION_JSON_VALUE)
    // @PreAuthorize("hasAnyRole('ROLE_STUDENT')")
    // @Operation(summary = "上传学生代码提交", description = "使用JSON格式上传学生实验代码文件")
    // public Result<ExperimentSubmissionDTO> uploadStudentCodeSubmission(
    //         @RequestBody CodeSubmissionDTO codeSubmission) {

    //     // 从安全上下文获取当前学生ID
    //     String studentId = getCurrentUserId();
    //     String taskId = codeSubmission.getTaskId();

    //     // 验证提交内容
    //     if (taskId == null || codeSubmission.getExperimentId() == null) {
    //         return Result.failure(400, "任务ID和实验ID不能为空");
    //     }

    //     // 调用学生实验服务提交任务
    //     ExperimentSubmissionDTO submissionDTO = studentExperimentService.submitTask(taskId, codeSubmission, studentId);
    //     return Result.success(submissionDTO);
    // }

    @GetMapping("/submissions/student/{studentId}")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN','ROLE_ASSISTANT') or #studentId == authentication.principal.id")
    @Operation(summary = "获取学生提交列表", description = "获取指定学生的所有提交")
    public Result<List<ResourceDTO>> getStudentSubmissions(@PathVariable String studentId) {
        List<ResourceDTO> submissions = resourceService.getStudentSubmissions(studentId);
        return Result.success(submissions);
    }

    @GetMapping("/submissions/student/{studentId}/experiment/{expId}")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN','ROLE_ASSISTANT') or #studentId == authentication.principal.id")
    @Operation(summary = "获取学生实验提交列表", description = "获取指定学生的特定实验提交")
    public Result<List<ResourceDTO>> getStudentExperimentSubmissions(
            @PathVariable String studentId,
            @PathVariable String expId) {
        List<ResourceDTO> submissions = resourceService.getStudentSubmissionsByExperiment(studentId, expId);
        return Result.success(submissions);
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     * @throws IllegalStateException 如果用户未认证
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User not authenticated");
        }
        return authentication.getName();
    }

    // @Deprecated
    // @PostMapping(value = "/submissions/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // @PreAuthorize("hasAnyRole('ROLE_STUDENT')")
    // @Operation(summary = "上传学生提交文件（已废弃）", description = "上传学生实验提交文件，该方法已废弃，请使用JSON格式的/submissions/upload接口")
    // public Result<ResourceDTO> uploadStudentSubmissionFile(
    //         @RequestParam("file") MultipartFile file,
    //         @RequestParam String experimentId,
    //         @RequestParam String taskId,
    //         @RequestParam(required = false) String description) {

    //     // 从安全上下文获取当前学生ID
    //     String studentId = getCurrentUserId();
    //     ResourceRequestDTO requestDTO = ResourceRequestDTO.builder()
    //             .experimentId(experimentId)
    //             .taskId(taskId)
    //             .description(description)
    //             .uploadType("resource") // Student submissions are always resources
    //             .autoExtract(false) // Student submissions should not be auto-extracted
    //             .build();

    //     ResourceDTO resource = resourceService.uploadStudentSubmission(file, studentId, experimentId, taskId, requestDTO);
    //     Result<ResourceDTO> result = Result.success(resource);
    //     result.setMessage("警告：此API已废弃，请尽快迁移到使用JSON格式的/submissions/upload接口");
    //     return result;
    // }
}
