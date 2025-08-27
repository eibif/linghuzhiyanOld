package org.linghu.mybackend.repository;

import org.linghu.mybackend.domain.ExperimentEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * 实验评测仓库
 */
@Repository
public interface ExperimentEvaluationRepository extends JpaRepository<ExperimentEvaluation, String> {
    
    /**
     * 根据提交ID查找评测结果
     * 
     * @param submissionId 提交ID
     * @return 评测结果列表
     */
    List<ExperimentEvaluation> findBySubmissionId(String submissionId);
    
    /**
     * 根据提交ID查找最新的评测结果
     * 
     * @param submissionId 提交ID
     * @return 最新的评测结果
     */
    Optional<ExperimentEvaluation> findFirstBySubmissionIdOrderByIdDesc(String submissionId);

    /**
     * 根据学生ID和任务ID查找评测结果
     * 
     * @param studentId 学生ID
     * @param taskId 任务ID
     * @return 评测结果列表
     */
    List<ExperimentEvaluation> findByUserIdAndTaskIdOrderByIdDesc(String studentId, String taskId);
}
