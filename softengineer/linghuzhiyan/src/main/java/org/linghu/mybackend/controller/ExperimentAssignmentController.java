package org.linghu.mybackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.dto.UserDTO;
import org.linghu.mybackend.service.ExperimentAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 实验分配API控制器
 */
@RestController
@RequestMapping("/api/experiments/assignments")
@PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
@Tag(name = "实验分配管理", description = "实验分配给学生相关API")
public class ExperimentAssignmentController {

    private final ExperimentAssignmentService experimentAssignmentService;

    @Autowired
    public ExperimentAssignmentController(ExperimentAssignmentService experimentAssignmentService) {
        this.experimentAssignmentService = experimentAssignmentService;
    }    
    @PostMapping("/{taskId}")
    @Operation(summary = "分配实验任务给学生", description = "将实验任务分配给特定学生")
    public Result<Void> assignTask(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> assignment) {

        Object userIdsObj = assignment.get("userIds");
        
        if (userIdsObj != null && userIdsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> userIds = (List<String>) userIdsObj;
            experimentAssignmentService.batchAssignTask(taskId, userIds);
        }
        else {
            throw new IllegalArgumentException("请求参数必须包含单个或多个学生");
        }
        
        return Result.success();
    }

    @PostMapping("/{taskId}/all")
    @Operation(summary = "分配实验任务给全部学生", description = "将实验任务分配给所有学生")
    public Result<Void> assignTaskToAllStudents(@PathVariable String taskId) {
        experimentAssignmentService.assignTaskToAllStudents(taskId);
        return Result.success();
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "获取实验任务分配列表", description = "获取实验任务分配给的学生列表")
    public Result<List<UserDTO>> getTaskAssignments(@PathVariable String taskId) {
        List<UserDTO> users = experimentAssignmentService.getTaskAssignments(taskId);
        return Result.success(users);
    }    @DeleteMapping("/{taskId}")
    @Operation(summary = "取消实验任务分配", description = "取消将实验任务分配给特定学生")
    public Result<Void> removeTaskAssignment(
            @RequestBody Map<String, Object> request,
            @PathVariable String taskId) {

        Object userIdsObj = request.get("userIds");
        
        if (userIdsObj != null && userIdsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> userIds = (List<String>) userIdsObj;
            experimentAssignmentService.batchRemoveTaskAssignment(taskId, userIds);
        }
        else {
            throw new IllegalArgumentException("请求参数必须包含单个或多个学生ID");
        }

        return Result.success();
    }
}
