package org.linghu.mybackend.repository;

import org.linghu.mybackend.domain.Experiment;
import org.linghu.mybackend.domain.Experiment.ExperimentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 实验仓储接口，负责实验数据的持久化
 */
@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, String> {

    /**
     * 根据创建者ID查找实验
     *
     * @param creatorId 创建者ID
     * @return 实验列表
     */
    List<Experiment> findByCreatorId(String creatorId);
    
    /**
     * 根据创建者ID分页查找实验
     *
     * @param creatorId 创建者ID
     * @param pageable 分页参数
     * @return 实验分页结果
     */
    Page<Experiment> findByCreatorId(String creatorId, Pageable pageable);

    /**
     * 根据状态查找实验
     *
     * @param status 实验状态
     * @return 实验列表
     */
    List<Experiment> findByStatus(ExperimentStatus status);
    
    /**
     * 根据状态分页查找实验
     *
     * @param status 实验状态
     * @param pageable 分页参数
     * @return 实验分页结果
     */
    Page<Experiment> findByStatus(ExperimentStatus status, Pageable pageable);

    /**
     * 根据创建者ID和状态查找实验
     *
     * @param creatorId 创建者ID
     * @param status 实验状态
     * @return 实验列表
     */
    List<Experiment> findByCreatorIdAndStatus(String creatorId, ExperimentStatus status);
    
    /**
     * 根据创建者ID和状态分页查找实验
     *
     * @param creatorId 创建者ID
     * @param status 实验状态
     * @param pageable 分页参数
     * @return 实验分页结果
     */
    Page<Experiment> findByCreatorIdAndStatus(String creatorId, ExperimentStatus status, Pageable pageable);

    /**
     * 根据名称模糊查询实验
     *
     * @param name 实验名称（部分匹配）
     * @return 实验列表
     */
    List<Experiment> findByNameContaining(String name);
    
    /**
     * 根据名称模糊分页查询实验
     *
     * @param name 实验名称（部分匹配）
     * @param pageable 分页参数
     * @return 实验分页结果
     */
    Page<Experiment> findByNameContaining(String name, Pageable pageable);

    /**
     * 查询在指定时间范围内进行的实验
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 实验列表
     */
    @Query("SELECT e FROM Experiment e WHERE e.startTime <= :endDate AND e.endTime >= :startDate")
    List<Experiment> findExperimentsInTimeRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime  endDate);
    
    /**
     * 查询当前有效的实验（已发布且在有效期内）
     *
     * @param currentDate 当前日期
     * @return 实验列表
     */
    @Query("SELECT e FROM Experiment e WHERE e.status = 'PUBLISHED' AND e.startTime <= :currentDate AND e.endTime >= :currentDate")
    List<Experiment> findActiveExperiments(@Param("currentDate") LocalDateTime currentDate);
    
    /**
     * 分页查询当前有效的实验（已发布且在有效期内）
     *
     * @param currentDate 当前日期
     * @param pageable 分页参数
     * @return 实验分页结果
     */
    @Query("SELECT e FROM Experiment e WHERE e.status = 'PUBLISHED' AND e.startTime <= :currentDate AND e.endTime >= :currentDate")
    Page<Experiment> findActiveExperiments(@Param("currentDate") LocalDateTime currentDate, Pageable pageable);
    
    /**
     * 根据ID和创建者ID查找实验
     *
     * @param id 实验ID
     * @param creatorId 创建者ID
     * @return 实验（可选）
     */
    Optional<Experiment> findByIdAndCreatorId(String id, String creatorId);
}
