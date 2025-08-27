package org.linghu.mybackend.service;

import org.linghu.mybackend.dto.ResourceDTO;
import org.linghu.mybackend.dto.ResourceRequestDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资源管理服务接口
 */
public interface ResourceService {
    
    /**
     * 上传资源
     * 
     * @param file 文件
     * @param requestDTO 资源请求DTO
     * @return 上传的资源DTO
     */
    ResourceDTO uploadResource(MultipartFile file, ResourceRequestDTO requestDTO);

    /**
     * 删除资源
     *
     * @param id 资源ID
     */
    void deleteResource(String id);

    /**
     * 更新资源信息
     *
     * @param resourceId 资源ID
     * @param requestDTO 资源DTO
     * @return 更新后的资源DTO
     */
    ResourceDTO updateResource(String resourceId,ResourceRequestDTO requestDTO);

    /**
     * 根据实验ID获取资源列表
     *
     * @param experimentId 实验ID
     * @return 资源DTO列表
     */
    List<ResourceDTO> getResourcesByExperimentId(String experimentId);

    /**
     * 获取所有资源列表
     *
     * @return 资源DTO列表
     */
    List<ResourceDTO> getAllResources();
    /**
     * 根据ID获取资源详情
     * 
     * @param id 资源ID
     * @return 资源DTO
     */
    ResourceDTO getResourceById(String id);

    /**
     *  下载资源
     *
     * @param id 资源ID
     * @return 资源文件
     */
    Resource downloadResource(String id);

    /**
     * 上传学生实验提交
     *
     * @param file 文件
     * @param studentId 学生ID
     * @param experimentId 实验ID
     * @param taskId 任务ID
     * @param requestDTO 资源请求DTO
     * @return 上传的资源DTO
     */
    ResourceDTO uploadStudentSubmission(MultipartFile file, String studentId, 
                                      String experimentId, String taskId, 
                                      ResourceRequestDTO requestDTO);

    /**
     * 获取学生所有实验提交
     *
     * @param studentId 学生ID
     * @return 资源DTO列表
     */
    List<ResourceDTO> getStudentSubmissions(String studentId);

    /**
     * 获取学生特定实验的提交
     *
     * @param studentId 学生ID
     * @param experimentId 实验ID
     * @return 资源DTO列表
     */
    List<ResourceDTO> getStudentSubmissionsByExperiment(String studentId, String experimentId);
}
