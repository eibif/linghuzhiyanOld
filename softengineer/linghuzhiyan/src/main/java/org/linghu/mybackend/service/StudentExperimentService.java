package org.linghu.mybackend.service;

import org.linghu.mybackend.dto.ExperimentDTO;
import org.linghu.mybackend.dto.ExperimentSubmissionDTO;
import org.linghu.mybackend.dto.ExperimentEvaluationDTO;
import org.linghu.mybackend.dto.ExperimentTaskDTO;
import org.linghu.mybackend.dto.SubmissionRequestDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * 学生实验参与服务接口
 */
public interface StudentExperimentService {
    
    /**
     * 获取学生可访问的实验列表
     * 
     * @param username 用户名
     * @return 实验DTO列表
     */
    List<ExperimentDTO> getStudentExperiments(String username);
    
    /**
     * 获取实验详情
     * 
     * @param expId 实验ID
     * @return 实验DTO
     */
    ExperimentDTO getExperimentDetails(String expId);
    
    /**
     * 获取分配给学生的所有实验任务列表
     * 
     * @param username 用户名
     * @return 任务DTO列表
     */
    List<ExperimentTaskDTO> getAssignedTasks(String username);
    
    /**
     * 获取特定任务的详情
     * 
    * @param taskId 任务ID
     * @param username 用户名
     * @return 任务DTO
     */
    ExperimentTaskDTO getTaskById(String taskId, String username);
    
    /**
     * 提交实验任务（使用SubmissionRequestDTO）
     * 
     * @param submissionRequest 提交请求DTO
     * @param username 学生用户名
     * @return 任务提交DTO
     */
    ExperimentSubmissionDTO submitTask(SubmissionRequestDTO submissionRequest, String username);

    /**
     * 获取实验任务评测结果
     * 
     * @param taskId 任务ID
     * @param username 用户名
     * @return 评测结果DTO
     */
    ExperimentEvaluationDTO getTaskEvaluationResult(String taskId, String username);
    
    /**
     * 获取实验任务历史评测记录
     * 
     * @param taskId 任务ID
     * @param username 用户名
     * @return 评测结果DTO列表
     */
    List<ExperimentEvaluationDTO> getTaskEvaluationHistory(String taskId, String username);
}
