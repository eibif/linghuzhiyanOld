package org.linghu.mybackend.repository;

import org.linghu.mybackend.domain.ExperimentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 实验分配仓储接口，负责实验任务分配数据的持久化
 */
@Repository
public interface ExperimentAssignmentRepository extends JpaRepository<ExperimentAssignment, String> {

    /**
     * 根据任务ID查询所有分配记录
     * 
     * @param taskId 任务ID
     * @return 分配记录列表
     */
    List<ExperimentAssignment> findByTaskId(String taskId);

    /**
     * 根据用户ID查询所有分配给该用户的任务分配记录
     * 
     * @param userId 用户ID
     * @return 分配记录列表
     */
    List<ExperimentAssignment> findByUserId(String userId);

    /**
     * 查询特定任务分配给特定用户的记录
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 分配记录（如果存在）
     */
    Optional<ExperimentAssignment> findByTaskIdAndUserId(String taskId, String userId);

    /**
     * 删除特定任务分配给特定用户的记录
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 删除的记录数
     */
    long deleteByTaskIdAndUserId(String taskId, String userId);

    /**
     * 删除任务的所有分配记录
     * 
     * @param taskId 任务ID
     * @return 删除的记录数
     */
    long deleteByTaskId(String taskId);

    /**
     * 统计任务的分配人数
     * 
     * @param taskId 任务ID
     * @return 分配人数
     */
    long countByTaskId(String taskId);

    /**
     * 检查任务是否分配给了特定用户
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否存在分配记录
     */
    boolean existsByTaskIdAndUserId(String taskId, String userId);

    /**
     * 根据任务ID查询分配的用户ID列表
     * 
     * @param taskId 任务ID
     * @return 用户ID列表
     */
    @Query("SELECT a.userId FROM ExperimentAssignment a WHERE a.taskId = :taskId")
    List<String> findUserIdsByTaskId(@Param("taskId") String taskId);

    /**
     * 根据用户ID查询分配给该用户的任务ID列表
     * 
     * @param userId 用户ID
     * @return 任务ID列表
     */
    @Query("SELECT a.taskId FROM ExperimentAssignment a WHERE a.userId = :userId")
    List<String> findTaskIdsByUserId(@Param("userId") String userId);
}
