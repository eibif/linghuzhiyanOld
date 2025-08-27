package org.linghu.mybackend.repository;

import org.linghu.mybackend.domain.ExperimentSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 实验提交仓储接口，负责实验提交数据的持久化
 */
@Repository
public interface ExperimentSubmissionRepository extends JpaRepository<ExperimentSubmission, String> {

    /**
     * 根据任务ID查找提交记录
     *
     * @param taskId 任务ID
     * @return 提交记录列表
     */
    List<ExperimentSubmission> findByTaskId(String taskId);
    
    /**
     * 根据任务ID分页查询提交记录
     *
     * @param taskId 任务ID
     * @param pageable 分页参数
     * @return 提交记录分页结果
     */
    Page<ExperimentSubmission> findByTaskId(String taskId, Pageable pageable);
    
    /**
     * 根据任务ID和提交时间范围查询提交记录
     *
     * @param taskId 任务ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 提交记录列表
     */
    List<ExperimentSubmission> findByTaskIdAndSubmitTimeBetween(String taskId, Date startDate, Date endDate);
    
    /**
     * 根据评分人ID查询提交记录
     *
     * @param graderId 评分人ID
     * @return 提交记录列表
     */
    List<ExperimentSubmission> findByGraderId(String graderId);
    
    /**
     * 查询未评分的提交记录
     *
     * @param taskId 任务ID
     * @return 未评分的提交记录列表
     */
    @Query("SELECT es FROM ExperimentSubmission es WHERE es.taskId = :taskId AND es.graderId IS NULL")
    List<ExperimentSubmission> findUngradedSubmissionsByTaskId(@Param("taskId") String taskId);
    
    /**
     * 查询已评分的提交记录
     *
     * @param taskId 任务ID
     * @return 已评分的提交记录列表
     */
    @Query("SELECT es FROM ExperimentSubmission es WHERE es.taskId = :taskId AND es.graderId IS NOT NULL")
    List<ExperimentSubmission> findGradedSubmissionsByTaskId(@Param("taskId") String taskId);
    
    /**
     * 查询指定时间范围内的提交记录
     *
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 提交记录列表
     */
    List<ExperimentSubmission> findBySubmitTimeBetween(Date startDate, Date endDate);
    
    /**
     * 根据任务ID和用户答案内容模糊查询提交记录
     *
     * @param taskId 任务ID
     * @param userAnswerContent 用户答案内容（部分匹配）
     * @return 提交记录列表
     */
    @Query("SELECT es FROM ExperimentSubmission es WHERE es.taskId = :taskId AND es.userAnswer LIKE %:content%")
    List<ExperimentSubmission> findByTaskIdAndUserAnswerContaining(@Param("taskId") String taskId, @Param("content") String userAnswerContent);
    
    /**
     * 查询任务的平均得分
     *
     * @param taskId 任务ID
     * @return 平均得分
     */
    @Query("SELECT AVG(es.score) FROM ExperimentSubmission es WHERE es.taskId = :taskId AND es.score IS NOT NULL")
    Double getAverageScoreByTaskId(@Param("taskId") String taskId);
      /**
     * 查询用户在特定任务上的最新提交记录
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 提交记录（可选）
     */
    @Query("SELECT es FROM ExperimentSubmission es WHERE es.taskId = :taskId AND es.userId = :userId ORDER BY es.submitTime DESC")
    Optional<ExperimentSubmission> findByTaskIdAndUserId(@Param("taskId") String taskId, @Param("userId") String userId);
    
    /**
     * 根据用户ID查找最新的提交记录
     *
     * @param userId 用户ID
     * @return 提交记录（如果存在）
     */
    Optional<ExperimentSubmission> findFirstByUserIdOrderBySubmitTimeDesc(String userId);
    
    /**
     * 根据用户ID和实验ID查找所有提交记录
     * 
     * @param userId 用户ID
     * @param experimentId 实验ID
     * @return 提交记录列表
     */
    @Query("SELECT es FROM ExperimentSubmission es JOIN ExperimentTask et ON es.taskId = et.id WHERE es.userId = :userId AND et.experimentId = :experimentId ORDER BY es.submitTime DESC")
    List<ExperimentSubmission> findByUserIdAndExperimentIdOrderBySubmitTimeDesc(@Param("userId") String userId, @Param("experimentId") String experimentId);
    
    /**
     * 根据用户ID查找所有提交记录
     *
     * @param userId 用户ID
     * @return 提交记录列表
     */
    List<ExperimentSubmission> findByUserId(String userId);
    
    /**
     * 根据用户ID分页查询提交记录
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 提交记录分页结果
     */
    Page<ExperimentSubmission> findByUserId(String userId, Pageable pageable);
}
