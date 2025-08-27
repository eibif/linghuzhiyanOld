package org.linghu.mybackend.service.impl;

import io.minio.Result;
import io.minio.messages.Item;
import org.linghu.mybackend.domain.Resource;
import org.linghu.mybackend.dto.ResourceDTO;
import org.linghu.mybackend.dto.ResourceRequestDTO;
import org.linghu.mybackend.repository.ExperimentRepository;
import org.linghu.mybackend.repository.ResourceRepository;
import org.linghu.mybackend.service.ResourceService;
import org.linghu.mybackend.util.MinioUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 资源管理服务实现类 - 基于MinIO实现文件存储
 */
@Service
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final ExperimentRepository experimentRepository;
    private final MinioUtil minioUtil;

    @Autowired
    public ResourceServiceImpl(
            ResourceRepository resourceRepository,
            ExperimentRepository experimentRepository,
            MinioUtil minioUtil) {
        this.resourceRepository = resourceRepository;
        this.experimentRepository = experimentRepository;
        this.minioUtil = minioUtil;
    }

    @Override
    @Transactional
    public ResourceDTO uploadResource(MultipartFile file, ResourceRequestDTO requestDTO) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件为空，无法上传");
        }

        // 验证实验是否存在
        if (requestDTO.getExperimentId() != null) {
            experimentRepository.findById(requestDTO.getExperimentId())
                    .orElseThrow(() -> new RuntimeException("实验不存在"));
        }

        try {
            // 确保存储桶存在
            minioUtil.ensureBucketExists();

            // 获取文件信息
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

            // 自动检测资源类型
            String resourceType = detectResourceType(originalFilename, file.getContentType());
            String objectName;
            List<String> uploadedPaths = new ArrayList<>();

            // 根据是否有实验ID选择不同的上传路径
            if (requestDTO.getExperimentId() != null) {
                // 上传到实验资源目录，根据uploadType参数确定资源类型
                String uploadResourceType = requestDTO.getUploadType() != null ? requestDTO.getUploadType()
                        : "resource"; // 默认为学习资料

                // 检查是否需要自动解压
                boolean shouldAutoExtract = "experiment".equals(uploadResourceType) &&
                        Boolean.TRUE.equals(requestDTO.getAutoExtract()) &&
                        minioUtil.isCompressedFile(originalFilename);
                if (shouldAutoExtract) {
                    // 自动解压压缩包
                    uploadedPaths = minioUtil.extractAndUploadCompressedFile(
                            requestDTO.getExperimentId(),
                            requestDTO.getTaskId(),
                            file.getInputStream(),
                            originalFilename,
                            file.getContentType());

                    // 如果成功解压了文件，使用第一个解压文件的路径作为主要路径
                    if (!uploadedPaths.isEmpty()) {
                        objectName = uploadedPaths.get(0); // 使用第一个解压出的文件作为主要路径
                    } else {
                        // 如果没有解压出文件，则抛出异常
                        throw new RuntimeException("解压文件失败，未找到有效内容");
                    }
                    // 不保存原始压缩包
                } else {
                    // 常规上传
                    objectName = minioUtil.uploadExperimentResource(
                            requestDTO.getExperimentId(),
                            originalFilename,
                            file.getInputStream(),
                            file.getSize(),
                            file.getContentType(),
                            uploadResourceType);
                    uploadedPaths.add(objectName);
                }
            } else { // 上传到通用资源目录
                objectName = minioUtil.uploadResource(
                        resourceType.toLowerCase(),
                        originalFilename,
                        file.getInputStream(),
                        file.getSize(),
                        file.getContentType());
                uploadedPaths.add(objectName);
            }

            // 创建资源记录 - 对于解压文件，创建主记录
            Resource resource = Resource.builder()
                    .id(UUID.randomUUID().toString())
                    .experimentId(requestDTO.getExperimentId())
                    .resourceType(Resource.ResourceType.valueOf(resourceType))
                    .resourcePath(objectName)
                    .fileName(originalFilename)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .description(requestDTO.getDescription())
                    .build();

            Resource savedResource = resourceRepository.save(resource);

            // 如果有多个文件（解压情况），创建额外的记录
            if (uploadedPaths.size() > 1) {
                for (int i = 1; i < uploadedPaths.size(); i++) {
                    String extractedPath = uploadedPaths.get(i);
                    String extractedFileName = extractedPath.substring(extractedPath.lastIndexOf("/") + 1);

                    Resource extractedResource = Resource.builder()
                            .id(UUID.randomUUID().toString())
                            .experimentId(requestDTO.getExperimentId())
                            .resourceType(detectResourceTypeFromPath(extractedPath))
                            .resourcePath(extractedPath)
                            .fileName(extractedFileName)
                            .fileSize(0L) // 解压后的文件大小需要单独获取
                            .mimeType(detectMimeTypeFromPath(extractedPath))
                            .description("从压缩包 " + originalFilename + " 中解压")
                            .build();
                    resourceRepository.save(extractedResource);
                }
            }

            return convertToDTO(savedResource);

        } catch (Exception ex) {
            throw new RuntimeException("无法上传文件到MinIO: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<ResourceDTO> getResourcesByExperimentId(String experimentId) {
        List<Resource> resources = resourceRepository.findByExperimentId(experimentId);
        return resources.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ResourceDTO getResourceById(String id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
        return convertToDTO(resource);
    }

    @Override
    @Transactional
    public void deleteResource(String id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));

        try {
            // 从MinIO删除文件
            minioUtil.deleteFile(resource.getResourcePath());

        } catch (Exception ex) {
            // 记录错误但继续删除数据库记录
            System.err.println("从MinIO删除文件失败: " + ex.getMessage());
        }

        // 删除数据库记录
        resourceRepository.delete(resource);
    }

    @Override
    public List<ResourceDTO> getAllResources() {
        List<Resource> resources = resourceRepository.findAll();
        return resources.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResourceDTO updateResource(String resourceId, ResourceRequestDTO requestDTO) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("资源不存在"));

        // 更新资源信息
        if (requestDTO.getExperimentId() != null) {
            experimentRepository.findById(requestDTO.getExperimentId())
                    .orElseThrow(() -> new RuntimeException("实验不存在"));
            resource.setExperimentId(requestDTO.getExperimentId());
        }

        if (requestDTO.getResourceType() != null) {
            resource.setResourceType(Resource.ResourceType.valueOf(requestDTO.getResourceType()));
        }

        if (requestDTO.getDescription() != null) {
            resource.setDescription(requestDTO.getDescription());
        }
        Resource updatedResource = resourceRepository.save(resource);
        return convertToDTO(updatedResource);
    }

    @Override
    public org.springframework.core.io.Resource downloadResource(String id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));

        try {
            // 从MinIO下载文件
            return minioUtil.downloadFile(resource.getResourcePath());
        } catch (Exception ex) {
            throw new RuntimeException("从MinIO下载文件失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 生成文件的临时预览URL
     * 
     * @param id         资源ID
     * @param expiryTime URL过期时间(秒)
     * @return 临时访问URL
     */
    public String generatePreviewUrl(String id, int expiryTime) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));

        try {
            return minioUtil.generatePreviewUrl(resource.getResourcePath(), expiryTime);
        } catch (Exception ex) {
            throw new RuntimeException("生成预览链接失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 将资源实体转换为DTO
     * 
     * @param resource 资源实体
     * @return 资源DTO
     */
    private ResourceDTO convertToDTO(Resource resource) {
        return ResourceDTO.builder()
                .id(resource.getId())
                .experimentId(resource.getExperimentId())
                .resourceType(resource.getResourceType().toString())
                .resourcePath(resource.getResourcePath())
                .fileName(resource.getFileName())
                .fileSize(resource.getFileSize())
                .mimeType(resource.getMimeType())
                .description(resource.getDescription())
                .uploadTime(resource.getUploadTime())
                .build();
    }

    @Override
    @Transactional
    public ResourceDTO uploadStudentSubmission(MultipartFile file, String studentId,
            String experimentId, String taskId,
            ResourceRequestDTO requestDTO) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件为空，无法上传");
        }

        // 验证实验是否存在
        if (experimentId != null) {
            experimentRepository.findById(experimentId)
                    .orElseThrow(() -> new RuntimeException("实验不存在"));
        }

        try {
            // 确保存储桶存在
            minioUtil.ensureBucketExists();

            // 获取文件信息
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

            // 上传文件到MinIO的学生提交目录
            String objectName = minioUtil.uploadStudentSubmission(
                    studentId,
                    experimentId,
                    taskId,
                    originalFilename,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType());
            // 创建资源记录
            Resource resource = Resource.builder()
                    .id(UUID.randomUUID().toString())
                    .experimentId(experimentId)
                    .resourceType(Resource.ResourceType.SUBMISSION)
                    .resourcePath(objectName) // MinIO对象名作为资源路径
                    .fileName(originalFilename)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .description(requestDTO.getDescription())
                    .build();

            Resource savedResource = resourceRepository.save(resource);

            return convertToDTO(savedResource);

        } catch (Exception ex) {
            throw new RuntimeException("无法上传学生提交文件: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<ResourceDTO> getStudentSubmissions(String studentId) {
        try {
            Iterable<Result<Item>> results = minioUtil.listStudentSubmissions(studentId);
            List<ResourceDTO> submissions = new java.util.ArrayList<>();

            for (Result<Item> result : results) {
                try {
                    Item item = result.get();
                    String objectName = item.objectName();

                    // 查找对应的资源记录
                    List<Resource> resources = resourceRepository.findByResourcePath(objectName);
                    if (!resources.isEmpty()) {
                        submissions.add(convertToDTO(resources.get(0)));
                    } else {
                        // 如果数据库中没有记录，创建一个基于文件信息的临时DTO
                        ResourceDTO tempDTO = createTemporaryDTO(item);
                        if (tempDTO != null) {
                            submissions.add(tempDTO);
                        }
                    }
                } catch (Exception e) {
                    // 跳过无法处理的项目
                    System.err.println("处理学生提交项目时出错: " + e.getMessage());
                }
            }

            return submissions;
        } catch (Exception ex) {
            throw new RuntimeException("获取学生提交列表失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<ResourceDTO> getStudentSubmissionsByExperiment(String studentId, String experimentId) {
        try {
            Iterable<Result<Item>> results = minioUtil.listStudentSubmissionsForExperiment(studentId, experimentId);
            List<ResourceDTO> submissions = new java.util.ArrayList<>();

            for (Result<Item> result : results) {
                try {
                    Item item = result.get();
                    String objectName = item.objectName();

                    // 查找对应的资源记录
                    List<Resource> resources = resourceRepository.findByResourcePath(objectName);
                    if (!resources.isEmpty()) {
                        submissions.add(convertToDTO(resources.get(0)));
                    } else {
                        // 如果数据库中没有记录，创建一个基于文件信息的临时DTO
                        ResourceDTO tempDTO = createTemporaryDTO(item);
                        if (tempDTO != null) {
                            submissions.add(tempDTO);
                        }
                    }
                } catch (Exception e) {
                    // 跳过无法处理的项目
                    System.err.println("处理学生提交项目时出错: " + e.getMessage());
                }
            }

            return submissions;
        } catch (Exception ex) {
            throw new RuntimeException("获取学生特定实验提交列表失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 根据MinIO对象元数据创建临时资源DTO
     * 
     * @param item MinIO对象元数据
     * @return 资源DTO
     */
    private ResourceDTO createTemporaryDTO(Item item) {
        try {
            String objectName = item.objectName();
            String fileName = objectName.substring(objectName.lastIndexOf("/") + 1);

            // 从路径中提取实验ID
            String experimentId = minioUtil.getExperimentIdFromPath(objectName);

            // 转换上传时间
            java.time.LocalDateTime uploadTime = java.time.LocalDateTime.now();
            if (item.lastModified() != null) {
                uploadTime = item.lastModified().toLocalDateTime();
            }
            return ResourceDTO.builder()
                    .id("temp-" + UUID.randomUUID().toString())
                    .experimentId(experimentId)
                    .resourceType("SUBMISSION")
                    .resourcePath(objectName)
                    .fileName(fileName)
                    .fileSize(item.size())
                    .mimeType(getMimeTypeFromFileName(fileName))
                    .description("自动生成的临时记录")
                    .uploadTime(uploadTime)
                    .build();
        } catch (Exception e) {
            System.err.println("创建临时DTO失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据文件名获取MIME类型
     * 
     * @param fileName 文件名
     * @return MIME类型
     */
    private String getMimeTypeFromFileName(String fileName) {
        String extension = "";
        if (fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        }

        switch (extension) {
            case ".pdf":
                return "application/pdf";
            case ".doc":
                return "application/msword";
            case ".docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls":
                return "application/vnd.ms-excel";
            case ".xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".ppt":
                return "application/vnd.ms-powerpoint";
            case ".pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".txt":
                return "text/plain";
            case ".zip":
                return "application/zip";
            case ".rar":
                return "application/x-rar-compressed";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 根据文件名和MIME类型自动检测资源类型
     * 
     * @param fileName 文件名
     * @param mimeType MIME类型
     * @return 资源类型
     */
    private String detectResourceType(String fileName, String mimeType) {
        if (fileName == null) {
            return "DOCUMENT";
        }

        String lowerFileName = fileName.toLowerCase();

        // 根据文件扩展名判断资源类型
        if (lowerFileName.endsWith(".pdf")) {
            return "DOCUMENT";
        } else if (lowerFileName.endsWith(".doc") || lowerFileName.endsWith(".docx") ||
                lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".md")) {
            return "DOCUMENT";
        } else if (lowerFileName.endsWith(".ppt") || lowerFileName.endsWith(".pptx")) {
            return "PRESENTATION";
        } else if (lowerFileName.endsWith(".xls") || lowerFileName.endsWith(".xlsx") ||
                lowerFileName.endsWith(".csv")) {
            return "SPREADSHEET";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg") ||
                lowerFileName.endsWith(".png") || lowerFileName.endsWith(".gif") ||
                lowerFileName.endsWith(".bmp")) {
            return "IMAGE";
        } else if (lowerFileName.endsWith(".mp4") || lowerFileName.endsWith(".avi") ||
                lowerFileName.endsWith(".mov") || lowerFileName.endsWith(".flv") ||
                lowerFileName.endsWith(".wmv")) {
            return "VIDEO";
        } else if (lowerFileName.endsWith(".mp3") || lowerFileName.endsWith(".wav") ||
                lowerFileName.endsWith(".ogg") || lowerFileName.endsWith(".flac")) {
            return "AUDIO";
        } else if (lowerFileName.endsWith(".zip") || lowerFileName.endsWith(".rar") ||
                lowerFileName.endsWith(".7z") || lowerFileName.endsWith(".tar") ||
                lowerFileName.endsWith(".gz")) {
            return "ARCHIVE";
        } else if (lowerFileName.endsWith(".html") || lowerFileName.endsWith(".htm") ||
                lowerFileName.endsWith(".js") || lowerFileName.endsWith(".css") ||
                lowerFileName.endsWith(".json") || lowerFileName.endsWith(".xml")) {
            return "CODE";
        }

        // 如果无法判断，则根据MIME类型进一步判断
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return "IMAGE";
            } else if (mimeType.startsWith("video/")) {
                return "VIDEO";
            } else if (mimeType.startsWith("audio/")) {
                return "AUDIO";
            } else if (mimeType.startsWith("text/")) {
                return "DOCUMENT";
            } else if (mimeType.equals("application/pdf")) {
                return "DOCUMENT";
            }
        }
        // 默认为文档类型
        return "DOCUMENT";
    }

    /**
     * 根据文件路径检测资源类型
     * 
     * @param filePath 文件路径
     * @return 资源类型枚举
     */
    private Resource.ResourceType detectResourceTypeFromPath(String filePath) {
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        String detectedType = detectResourceType(fileName, null);
        return Resource.ResourceType.valueOf(detectedType);
    }

    /**
     * 根据文件路径检测MIME类型
     * 
     * @param filePath 文件路径
     * @return MIME类型
     */
    private String detectMimeTypeFromPath(String filePath) {
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        return getMimeTypeFromFileName(fileName);
    }
}
