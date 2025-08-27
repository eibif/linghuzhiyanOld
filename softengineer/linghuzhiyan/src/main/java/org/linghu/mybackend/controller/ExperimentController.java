package org.linghu.mybackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.linghu.mybackend.dto.PageResult;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.dto.ExperimentDTO;
import org.linghu.mybackend.dto.ExperimentRequestDTO;

import org.linghu.mybackend.service.ExperimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 实验管理API控制器
 */
@RestController
@RequestMapping("/api/experiments")
@Tag(name = "实验管理", description = "实验管理相关API")
@PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN','ROLE_ASSISTANT')")
public class ExperimentController {

    private final ExperimentService experimentService;

    @Autowired
    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @PostMapping
    @Operation(summary = "创建实验", description = "创建一个新的实验")
    public Result<ExperimentDTO> createExperiment(@Valid @RequestBody ExperimentRequestDTO requestDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        ExperimentDTO experiment = experimentService.createExperiment(requestDTO, userDetails.getUsername());
        return Result.success(experiment);
    }

    @GetMapping
    @Operation(summary = "获取实验列表", description = "分页获取实验列表")
    public Result<PageResult<ExperimentDTO>> getExperiments(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<ExperimentDTO> page;

        page = experimentService.getAllExperiments(pageNum, pageSize);

        PageResult<ExperimentDTO> pageResult = new PageResult<>();
        pageResult.setList(page.getContent());
        pageResult.setTotal(page.getTotalElements());
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);

        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取实验详情", description = "根据ID获取实验详细信息")
    public Result<ExperimentDTO> getExperiment(@PathVariable String id) {
        ExperimentDTO experiment = experimentService.getExperimentById(id);
        return Result.success(experiment);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新实验", description = "根据实验id更新实验信息")
    public Result<ExperimentDTO> updateExperiment(
            @PathVariable String id,
            @Valid @RequestBody ExperimentRequestDTO requestDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        ExperimentDTO updatedExperiment = experimentService.updateExperiment(id, requestDTO, userDetails.getUsername());
        return Result.success(updatedExperiment);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除实验", description = "删除指定的实验")
    public Result<Void> deleteExperiment(@PathVariable String id) {
        experimentService.deleteExperiment(id);
        return Result.success();
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "发布实验", description = "将实验状态改为已发布")
    public Result<ExperimentDTO> publishExperiment(@PathVariable String id) {
        ExperimentDTO experiment = experimentService.publishExperiment(id);
        return Result.success(experiment);
    }

    @PutMapping("/{id}/unpublish")
    @Operation(summary = "取消发布实验", description = "将实验状态改为草稿")
    public Result<ExperimentDTO> unpublishExperiment(@PathVariable String id) {
        ExperimentDTO experiment = experimentService.unpublishExperiment(id);
        return Result.success(experiment);
    }
}
