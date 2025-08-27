package org.linghu.mybackend.repository;

import org.linghu.mybackend.domain.Question;
import org.linghu.mybackend.domain.Question.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 题目仓储接口，负责题目数据的持久化
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, String> {
    
    /**
     * 根据题目类型查找题目
     * 
     * @param questionType 题目类型
     * @return 题目列表
     */
    List<Question> findByQuestionType(QuestionType questionType);
    
    /**
     * 根据题目类型分页查询题目
     * 
     * @param questionType 题目类型
     * @param pageable 分页参数
     * @return 题目分页结果
     */
    Page<Question> findByQuestionType(QuestionType questionType, Pageable pageable);

    /**
     * 根据标签模糊查询题目
     * 
     * @param tag 标签（包含部分）
     * @return 题目列表
     */
    @Query("SELECT q FROM Question q WHERE q.tags LIKE %:tag%")
    List<Question> findByTagsContaining(@Param("tag") String tag);
    
    /**
     * 根据内容模糊查询题目
     * 
     * @param content 内容（包含部分）
     * @return 题目列表
     */
    List<Question> findByContentContaining(String content);
    
    /**
     * 根据多个ID查询题目
     * 
     * @param ids ID列表
     * @return 题目列表
     */
    List<Question> findByIdIn(List<String> ids);
    
    /**
     * 获取特定题型的题目总数
     * 
     * @param questionType 题目类型
     * @return 题目数量
     */
    long countByQuestionType(QuestionType questionType);
    
    /**
     * 查询包含特定标签的题目集合
     * 
     * @param tags 标签列表
     * @return 题目列表
     */
    @Query(value = "SELECT q.* FROM question q WHERE (:tags IS NULL OR EXISTS (SELECT 1 FROM UNNEST(REGEXP_SPLIT_TO_ARRAY(q.tags, ',')) t WHERE TRIM(t) IN (:tags)))", nativeQuery = true)
    List<Question> findByTagsIn(@Param("tags") Set<String> tags);

   }
