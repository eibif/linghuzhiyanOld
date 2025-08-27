package org.linghu.mybackend.service;

import org.linghu.mybackend.dto.UserDTO;

import java.util.List;

/**
 * 实验任务分配管理服务接口
 */
public interface ExperimentAssignmentService {
    
    /**
     * 分配实验任务给学生
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    void assignTask(String taskId, String userId);
    
    /**
     * 批量分配实验任务给学生
     * 
     * @param taskId 任务ID
     * @param userIds 用户ID列表
     */
    void batchAssignTask(String taskId, List<String> userIds);
    
    /**
     * 分配实验任务给所有学生
     * 
     * @param taskId 任务ID
     */
    void assignTaskToAllStudents(String taskId);
    
    /**
     * 获取实验任务分配的学生列表
     * 
     * @param taskId 任务ID
     * @return 用户DTO列表
     */
    List<UserDTO> getTaskAssignments(String taskId);
    
    /**
     * 取消实验任务分配
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    void removeTaskAssignment(String taskId, String userId);
    
    /**
     * 批量取消实验任务分配
     * 
     * @param taskId 任务ID
     * @param userIds 用户ID列表
     */
    void batchRemoveTaskAssignment(String taskId, List<String> userIds);
}
