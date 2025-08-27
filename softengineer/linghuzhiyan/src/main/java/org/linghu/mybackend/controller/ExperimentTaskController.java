package org.linghu.mybackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.linghu.mybackend.dto.ExperimentTaskDTO;
import org.linghu.mybackend.dto.ExperimentTaskRequestDTO;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.service.ExperimentTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 实验任务API控制器
 */
@RequestMapping("/api/experiments/tasks")
@RestController
@PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN','ROLE_ASSISTANT')")
@Tag(name = "实验任务管理", description = "实验任务管理相关API")
public class ExperimentTaskController {

    private final ExperimentTaskService experimentTaskService;

    @Autowired
    public ExperimentTaskController(ExperimentTaskService experimentTaskService) {
        this.experimentTaskService = experimentTaskService;
    }

    @PostMapping("/{expId}")
    @Operation(summary = "添加实验任务", description = "为指定实验添加一个新任务")
    public Result<ExperimentTaskDTO> addTask(
            @PathVariable String expId,
            @Valid @RequestBody ExperimentTaskRequestDTO requestDTO) {
        ExperimentTaskDTO task = experimentTaskService.createTask(expId, requestDTO);
        return Result.success(task);
    }

    @GetMapping("/{expId}")
    @Operation(summary = "获取实验任务列表", description = "获取指定实验的所有任务")
    public Result<List<ExperimentTaskDTO>> getTasks(@PathVariable String expId) {
        List<ExperimentTaskDTO> tasks = experimentTaskService.getTasksByExperimentId(expId);        
        return Result.success(tasks);
    }

    @PutMapping("/{expId}/{taskId}")
    @Operation(summary = "更新任务", description = "更新指定任务的信息")
    public Result<ExperimentTaskDTO> updateTask(
            @PathVariable String expId,
            @PathVariable String taskId,
            @Valid @RequestBody ExperimentTaskRequestDTO requestDTO) {
        ExperimentTaskDTO updatedTask = experimentTaskService.updateTask(taskId, requestDTO);
        return Result.success(updatedTask);
    }

    @DeleteMapping("/{expId}/{taskId}")
    @Operation(summary = "删除任务", description = "删除指定的任务")
    public Result<Void> deleteTask(
            @PathVariable String expId,
            @PathVariable String taskId) {
        experimentTaskService.deleteTask(taskId);
        return Result.success();
    }
    
}
