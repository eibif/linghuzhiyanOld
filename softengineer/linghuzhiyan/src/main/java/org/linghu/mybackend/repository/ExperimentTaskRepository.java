package org.linghu.mybackend.repository;

import org.linghu.mybackend.domain.ExperimentTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 实验任务仓储接口，负责实验任务数据的持久化
 */
@Repository
public interface ExperimentTaskRepository extends JpaRepository<ExperimentTask, String> {

    /**
     * 根据实验ID查找任务
     *
     * @param experimentId 实验ID
     * @return 任务列表
     */
    List<ExperimentTask> findByExperimentId(String experimentId);
      /**
     * 根据实验ID按顺序查找任务
     *
     * @param experimentId 实验ID
     * @return 任务列表，按orderNum排序
     */
    List<ExperimentTask> findByExperimentIdOrderByOrderNum(String experimentId);
    
    /**
     * 根据实验ID按顺序升序查找任务
     *
     * @param experimentId 实验ID
     * @return 任务列表，按orderNum升序排序
     */
    List<ExperimentTask> findByExperimentIdOrderByOrderNumAsc(String experimentId);
    
    /**
     * 根据实验ID分页查询任务
     *
     * @param experimentId 实验ID
     * @param pageable 分页参数
     * @return 任务分页结果
     */
    Page<ExperimentTask> findByExperimentId(String experimentId, Pageable pageable);
    
    /**
     * 计算实验包含的任务数量
     *
     * @param experimentId 实验ID
     * @return 任务数量
     */
    long countByExperimentId(String experimentId);
    
    /**
     * 计算实验中必做任务的数量
     *
     * @param experimentId 实验ID
     * @return 必做任务数量
     */
    long countByExperimentIdAndRequiredTrue(String experimentId);
    
    /**
     * 查找实验中的必做任务
     *
     * @param experimentId 实验ID
     * @return 必做任务列表
     */
    List<ExperimentTask> findByExperimentIdAndRequiredTrue(String experimentId);
    
    /**
     * 查找实验中的选做任务
     *
     * @param experimentId 实验ID
     * @return 选做任务列表
     */
    List<ExperimentTask> findByExperimentIdAndRequiredFalse(String experimentId);
    
    /**
     * 根据实验ID和ID查找任务
     *
     * @param experimentId 实验ID
     * @param id 任务ID
     * @return 任务（可选）
     */
    Optional<ExperimentTask> findByExperimentIdAndId(String experimentId, String id);
    
    /**
     * 根据实验ID和标题查找任务
     *
     * @param experimentId 实验ID
     * @param title 任务标题
     * @return 任务（可选）
     */
    Optional<ExperimentTask> findByExperimentIdAndTitle(String experimentId, String title);
    
    /**
     * 查找实验中最大的任务顺序号
     *
     * @param experimentId 实验ID
     * @return 最大顺序号
     */
    @Query("SELECT COALESCE(MAX(t.orderNum), 0) FROM ExperimentTask t WHERE t.experimentId = :experimentId")
    int findMaxOrderNumByExperimentId(@Param("experimentId") String experimentId);
    
    /**
     * 删除实验的所有任务
     *
     * @param experimentId 实验ID
     * @return 删除的记录数
     */
    long deleteByExperimentId(String experimentId);
}
