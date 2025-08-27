package org.linghu.mybackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.linghu.mybackend.dto.ExperimentDTO;
import org.linghu.mybackend.dto.ExperimentTaskDTO;
import org.linghu.mybackend.dto.ExperimentEvaluationDTO;
import org.linghu.mybackend.dto.ExperimentSubmissionDTO;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.dto.SubmissionRequestDTO;
import org.linghu.mybackend.service.StudentExperimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生实验参与API控制器
 */
@RestController
@RequestMapping("/api/student/experiments")
@PreAuthorize("hasRole('ROLE_STUDENT')")
@Tag(name = "学生实验参与", description = "学生参与实验相关API")
public class StudentExperimentController {

    private final StudentExperimentService studentExperimentService;

    @Autowired
    public StudentExperimentController(StudentExperimentService studentExperimentService) {
        this.studentExperimentService = studentExperimentService;
    }

    @GetMapping
    @Operation(summary = "获取实验列表", description = "获取学生可以参与的实验列表")
    public Result<List<ExperimentDTO>> getExperiments(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ExperimentDTO> experiments = studentExperimentService.getStudentExperiments(userDetails.getUsername());
        return Result.success(experiments);
    }

    @GetMapping("/{expId}")
    @Operation(summary = "获取实验详情", description = "获取指定实验的详细信息")
    public Result<ExperimentDTO> getExperiment(
            @PathVariable String expId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ExperimentDTO experiment = studentExperimentService.getExperimentDetails(expId);
        return Result.success(experiment);
    }

    @GetMapping("/tasks")
    @Operation(summary = "获取已分配实验任务", description = "获取分配给学生的所有实验任务")
    public Result<List<ExperimentTaskDTO>> getAssignedTasks(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ExperimentTaskDTO> tasks = studentExperimentService.getAssignedTasks(userDetails.getUsername());
        return Result.success(tasks);
    }    
    
    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "获取具体任务", description = "获取特定任务的详细信息")
    public Result<ExperimentTaskDTO> getTask(
            @PathVariable String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ExperimentTaskDTO task = studentExperimentService.getTaskById(taskId, userDetails.getUsername());
        return Result.success(task);
    }
    
    @PostMapping("/tasks/submit")
    @Operation(summary = "提交任务", description = "使用SubmissionRequestDTO提交任务的答案或完成情况")
    public Result<ExperimentSubmissionDTO> submitTask(
            @Valid @RequestBody SubmissionRequestDTO submissionRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        ExperimentSubmissionDTO result = studentExperimentService.submitTask(submissionRequest, userDetails.getUsername());
        return Result.success(result);
    }

    @GetMapping("/tasks/{taskId}/result")
    @Operation(summary = "获取评测结果", description = "获取学生提交任务的评测结果")
    public Result<ExperimentEvaluationDTO> getTaskEvaluationResult(
            @PathVariable String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ExperimentEvaluationDTO evaluationResult = studentExperimentService.getTaskEvaluationResult(
                taskId, userDetails.getUsername());
        return Result.success(evaluationResult);
    }

    @GetMapping("/tasks/{taskId}/history")
    @Operation(summary = "获取特定实验历史评测记录", description = "获取学生特定任务的所有历史评测记录")
    public Result<List<ExperimentEvaluationDTO>> getTaskEvaluationHistory(
            @PathVariable String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ExperimentEvaluationDTO> evaluationHistory = studentExperimentService.getTaskEvaluationHistory(
                taskId, userDetails.getUsername());
        return Result.success(evaluationHistory);
    }
}
