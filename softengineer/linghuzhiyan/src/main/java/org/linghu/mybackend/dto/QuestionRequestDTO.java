package org.linghu.mybackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.linghu.mybackend.domain.Question.QuestionType;

import java.math.BigDecimal;

/**
 * 题目请求DTO，用于创建或更新题目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequestDTO {
    @NotNull(message = "题目类型不能为空")
    private QuestionType questionType;
    
    @NotBlank(message = "题目内容不能为空")
    private String content;
    
    private Object options;       // 选项内容，例如：选择题的选项
    private Object answer;        // 标准答案
    private String explanation;   // 解析说明
    private String tags;          // 标签，用逗号分隔

    @NotNull(message = "题目分值不能为空")
    private BigDecimal score; // 题目分值
}
