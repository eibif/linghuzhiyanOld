package org.linghu.mybackend.service;

import org.linghu.mybackend.dto.ExperimentTaskDTO;
import org.linghu.mybackend.dto.ExperimentTaskRequestDTO;

import java.util.List;
import java.util.Map;

/**
 * 实验任务管理服务接口
 */
public interface ExperimentTaskService {

    /**
     * 创建任务
     *
     * @param experimentId 实验ID
     * @param requestDTO   任务请求DTO
     * @return 创建的任务DTO
     */
    ExperimentTaskDTO createTask(String experimentId, ExperimentTaskRequestDTO requestDTO);

    /**
     * 根据实验ID获取任务列表
     *
     * @param experimentId 实验ID
     * @return 任务DTO列表
     */
    List<ExperimentTaskDTO> getTasksByExperimentId(String experimentId);

    /**
     * 更新任务
     *
     * @param id         任务ID
     * @param requestDTO 任务请求DTO
     * @return 更新后的任务DTO
     */
    ExperimentTaskDTO updateTask(String id, ExperimentTaskRequestDTO requestDTO);

    /**
     * 删除任务
     *
     * @param id 任务ID
     */
    void deleteTask(String id);

    /**
     * 调整任务顺序
     *
     * @param experimentId  实验ID
     * @param taskOrderList 任务顺序列表
     * @return 更新顺序后的任务DTO列表
     */
    List<ExperimentTaskDTO> adjustTaskOrder(String experimentId, List<Map<String, String>> taskOrderList);
}
