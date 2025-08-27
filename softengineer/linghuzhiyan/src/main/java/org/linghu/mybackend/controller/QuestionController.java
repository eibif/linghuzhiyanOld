package org.linghu.mybackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.linghu.mybackend.domain.Question.QuestionType;
import org.linghu.mybackend.dto.PageResult;
import org.linghu.mybackend.dto.QuestionDTO;
import org.linghu.mybackend.dto.QuestionRequestDTO;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题库管理API控制器
 */
@RestController
@RequestMapping("/api/questions")
@PreAuthorize("hasAnyRole('ROLE_TEACHER', 'ROLE_ADMIN', 'ROLE_ASSISTANT')")
@Tag(name = "题库管理", description = "题目管理相关API")
public class QuestionController {

    private final QuestionService questionService;

    @Autowired
    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    @Operation(summary = "创建题目", description = "创建新的题目")
    public Result<QuestionDTO> createQuestion(
            @Valid @RequestBody QuestionRequestDTO requestDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        QuestionDTO question = questionService.createQuestion(requestDTO, userDetails.getUsername());
        return Result.success(question);

    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除题目", description = "删除指定的题目")
    public Result<Void> deleteQuestion(@PathVariable String id) {
        questionService.deleteQuestion(id);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新题目", description = "更新指定题目的信息")
    public Result<QuestionDTO> updateQuestion(
            @PathVariable String id,
            @Valid @RequestBody QuestionRequestDTO requestDTO) {
        QuestionDTO question = questionService.updateQuestion(id, requestDTO);
        return Result.success(question);

    }

    @GetMapping("/{id}")
    @Operation(summary = "获取题目", description = "获取指定题目的详细信息")
    public Result<QuestionDTO> getQuestion(@PathVariable String id) {
        QuestionDTO question = questionService.getQuestionById(id);
        return Result.success(question);

    }    
    
    @GetMapping
    @Operation(summary = "分页查询题目", description = "根据条件分页查询题目")
    public Result<PageResult<QuestionDTO>> getQuestions(
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<QuestionDTO> pageResult = questionService.getQuestions(type, keyword, tags, 
                PageRequest.of(pageNum - 1, pageSize));
        
        // 转换为自定义的PageResult
        PageResult<QuestionDTO> result = PageResult.of(
                pageResult.getContent(),
                pageResult.getTotalElements(),
                pageNum,
                pageSize
        );
        
        return Result.success(result);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索题目", description = "根据关键词搜索题目")
    public Result<List<QuestionDTO>> searchQuestions(@RequestParam String keyword) {
        List<QuestionDTO> questions = questionService.searchQuestions(keyword);
        return Result.success(questions);

    }
}
