package org.linghu.mybackend.service;

import org.springframework.data.domain.Page;
import org.linghu.mybackend.dto.ExperimentDTO;
import org.linghu.mybackend.dto.ExperimentRequestDTO;

/**
 * 实验管理服务接口
 */
public interface ExperimentService {
    
    /**
     * 创建新实验
     * 
     * @param requestDTO 实验请求DTO
     * @param creatorUsername 创建者用户名
     * @return 创建的实验DTO
     */
    ExperimentDTO createExperiment(ExperimentRequestDTO requestDTO, String creatorUsername);
    
    /**
     * 获取所有实验（分页）
     * 
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 实验DTO分页
     */
    Page<ExperimentDTO> getAllExperiments(int pageNum, int pageSize);
    
    /**
     * 根据实验ID获取实验详情
     * 
     * @param id 实验ID
     * @return 实验DTO
     */
    ExperimentDTO getExperimentById(String id);
    
    /**
     * 更新实验
     * 
     * @param id 实验ID
     * @param requestDTO 实验请求DTO
     * @param username 当前用户名
     * @return 更新后的实验DTO
     */
    ExperimentDTO updateExperiment(String id, ExperimentRequestDTO requestDTO, String username);
    
    /**
     * 删除实验
     * 
     * @param id 实验ID
     */
    void deleteExperiment(String id);
    
    /**
     * 发布实验
     * 
     * @param id 实验ID
     * @return 发布后的实验DTO
     */
    ExperimentDTO publishExperiment(String id);
    
    /**
     * 取消发布实验
     * 
     * @param id 实验ID
     * @return 取消发布后的实验DTO
     */
    ExperimentDTO unpublishExperiment(String id);
}
