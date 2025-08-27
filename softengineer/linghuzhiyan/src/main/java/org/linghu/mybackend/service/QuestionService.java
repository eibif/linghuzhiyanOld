package org.linghu.mybackend.service;

import org.linghu.mybackend.domain.Question.QuestionType;
import org.linghu.mybackend.dto.QuestionDTO;
import org.linghu.mybackend.dto.QuestionRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

/**
 * 题库管理服务接口
 */
public interface QuestionService {

    /**
     * 创建题目
     * 
     * @param requestDTO 题目请求DTO
     * @param username 创建者用户名
     * @return 创建的题目DTO
     */
    QuestionDTO createQuestion(QuestionRequestDTO requestDTO, String username);
    
    /**
     * 根据ID获取题目
     * 
     * @param id 题目ID
     * @return 题目DTO
     */
    QuestionDTO getQuestionById(String id);
    
    /**
     * 更新题目
     * 
     * @param id 题目ID
     * @param requestDTO 题目请求DTO
     * @return 更新后的题目DTO
     */
    QuestionDTO updateQuestion(String id, QuestionRequestDTO requestDTO);
    
    /**
     * 删除题目
     * 
     * @param id 题目ID
     */
    void deleteQuestion(String id);
    
    /**
     * 分页查询题目
     * 
     * @param type 题目类型
     * @param keyword 关键词
     * @param tags 标签
     * @param pageable 分页参数
     * @return 题目DTO分页结果
     */
    Page<QuestionDTO> getQuestions(QuestionType type, String keyword, String tags, Pageable pageable);
    
    /**
     * 关键词搜索题目
     * 
     * @param keyword 关键词
     * @return 题目DTO列表
     */
    List<QuestionDTO> searchQuestions(String keyword);
    
    /**
     * 获取题目类型列表
     * 
     * @return 题目类型列表
     */
    List<QuestionType> getQuestionTypes();
    
    /**
     * 获取所有标签
     * 
     * @return 标签集合
     */
    Set<String> getAllTags();
    
    /**
     * 根据标签查找题目
     * 
     * @param tag 标签
     * @param pageable 分页参数
     * @return 题目DTO分页结果
     */
    Page<QuestionDTO> getQuestionsByTag(String tag, Pageable pageable);
    
    /**
     * 根据题目类型查找题目
     * 
     * @param type 题目类型
     * @param pageable 分页参数
     * @return 题目DTO分页结果
     */
    Page<QuestionDTO> getQuestionsByType(QuestionType type, Pageable pageable);
    
    /**
     * 批量获取题目
     * 
     * @param ids 题目ID列表
     * @return 题目DTO列表
     */
    List<QuestionDTO> getQuestionsByIds(List<String> ids);
}
