package org.linghu.mybackend.repository;

import org.linghu.mybackend.domain.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 资源仓储接口，负责资源数据的持久化
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, String> {
    
    /**
     * 根据实验ID查找资源
     * 
     * @param experimentId 实验ID
     * @return 资源列表
     */
    List<Resource> findByExperimentId(String experimentId);
    

    /**
     * 根据资源类型查找资源
     * 
     * @param resourceType 资源类型
     * @return 资源列表
     */
    List<Resource> findByResourceType(Resource.ResourceType resourceType);
    
    /**
     * 根据实验ID和资源类型查找资源
     * 
     * @param experimentId 实验ID
     * @param resourceType 资源类型
     * @return 资源列表
     */
    List<Resource> findByExperimentIdAndResourceType(String experimentId, Resource.ResourceType resourceType);
    

    /**
     * 根据文件名模糊搜索资源
     * 
     * @param fileName 文件名（部分）
     * @param pageable 分页信息
     * @return 资源分页
     */
    Page<Resource> findByFileNameContaining(String fileName, Pageable pageable);
    
    /**
     * 根据MIME类型查找资源
     * 
     * @param mimeType MIME类型
     * @return 资源列表
     */
    List<Resource> findByMimeType(String mimeType);
    
    /**
     * 检查实验是否有资源
     * 
     * @param experimentId 实验ID
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByExperimentId(String experimentId);

    /**
     * 根据资源路径查找资源
     * 
     * @param resourcePath 资源路径
     * @return 资源列表
     */
    List<Resource> findByResourcePath(String resourcePath);
}
