package org.linghu.mybackend.util;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.linghu.mybackend.config.MinioConfig;
import org.linghu.mybackend.dto.SourceCodeFileDTO;
import org.linghu.mybackend.dto.SubmissionRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 * MinIO操作工具类 - 提供基于MinIO的文件存储操作
 */
@Component
public class MinioUtil {
    private final MinioClient minioClient;
    private final MinioConfig minioConfig; // 定义不同的bucket名称
    private static final String SUBMISSION_BUCKET = "submission";
    private static final String RESOURCE_BUCKET = "resource";

    // 用户头像前缀（存储在默认bucket中）
    private static final String PREFIX_AVATARS = "avatars/";

    // 资源类型常量
    private static final String RESOURCE_TYPE_EXPERIMENT = "experiment";
    private static final String RESOURCE_TYPE_LEARNING = "resource";

    // 路径分隔符
    private static final String PATH_SEPARATOR = "/";

    // 时间戳格式
    private static final String TIMESTAMP_FORMAT = "yyyyMMdd-HHmmss";

    @Autowired
    public MinioUtil(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
    }

    /**
     * 确保MinIO存储桶存在，如不存在则创建
     * 
     * @throws Exception 如果操作失败
     */
    public void ensureBucketExists() throws Exception {
        ensureBucketExists(minioConfig.getBucketName());
        ensureBucketExists(SUBMISSION_BUCKET);
        ensureBucketExists(RESOURCE_BUCKET);
    }

    /**
     * 确保指定的bucket存在
     * 
     * @param bucketName bucket名称
     * @throws Exception 如果操作失败
     */
    private void ensureBucketExists(String bucketName) throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());

        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    /**
     * 上传文件到MinIO
     * 
     * @param fileStream  文件流
     * @param fileSize    文件大小
     * @param contentType 文件内容类型
     * @param extension   文件扩展名
     * @return MinIO中的对象名
     * @throws Exception 如果上传失败
     */
    private String uploadFile(InputStream fileStream, long fileSize, String contentType, String extension)
            throws Exception {
        // 生成通用文件名(存放在根目录下)
        String objectName = UUID.randomUUID().toString() + extension;

        // 上传文件到MinIO
        return uploadObject(objectName, fileStream, fileSize, contentType);
    }

    /**
     * 上传MultipartFile到MinIO
     * 
     * @param file      要上传的MultipartFile
     * @param extension 文件扩展名
     * @return MinIO中的对象名
     * @throws Exception 如果上传失败
     */
    public String uploadFile(MultipartFile file, String extension) throws Exception {
        return uploadFile(file.getInputStream(), file.getSize(), file.getContentType(), extension);
    }

    /**
     * 上传MultipartFile到MinIO的实验资源目录
     * 
     * @param file         要上传的MultipartFile
     * @param experimentId 实验ID
     * @param resourceType 资源类型("resource"表示学习资料，"experiment"表示源代码和评测脚本)
     * @return MinIO中的对象名
     * @throws Exception 如果上传失败
     */
    public String uploadExperimentFile(MultipartFile file, String experimentId, String resourceType) throws Exception {
        String filename = file.getOriginalFilename();
        return uploadExperimentResource(
                experimentId,
                filename,
                file.getInputStream(),
                file.getSize(),
                file.getContentType(),
                resourceType);
    }

    /**
     * 上传MultipartFile到MinIO的学生提交目录
     * 
     * @param file         要上传的MultipartFile
     * @param studentId    学生ID
     * @param experimentId 实验ID
     * @param taskId       任务ID
     * @return MinIO中的对象名
     * @throws Exception 如果上传失败
     */
    public String uploadSubmissionFile(MultipartFile file, String studentId,
            String experimentId, String taskId) throws Exception {
        String filename = file.getOriginalFilename();
        return uploadStudentSubmission(
                studentId,
                experimentId,
                taskId,
                filename,
                file.getInputStream(),
                file.getSize(),
                file.getContentType());
    }

    /**
     * 从MinIO下载文件
     * 
     * @param objectName MinIO中的对象名
     * @return 包含文件内容的InputStreamResource
     * @throws Exception 如果下载失败
     */
    public InputStreamResource downloadFile(String objectName) throws Exception {
        // 验证对象路径前缀合法性
//        validateObjectPrefix(objectName);

        // 根据对象路径确定使用哪个bucket
        String bucketName = determineBucketByObjectPath(objectName);

        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build();

        InputStream stream = minioClient.getObject(getObjectArgs);
        return new InputStreamResource(stream);
    }

    /**
     * 从MinIO删除文件
     * 
     * @param objectName MinIO中的对象名
     * @throws Exception 如果删除失败
     */
    public void deleteFile(String objectName) throws Exception {
        // 验证对象路径前缀合法性
        validateObjectPrefix(objectName);

        // 根据对象路径确定使用哪个bucket
        String bucketName = determineBucketByObjectPath(objectName);

        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    /**
     * 删除特定前缀下的所有文件
     * 
     * @param prefix 前缀路径
     * @throws Exception 如果删除失败
     */
    public void deleteByPrefix(String prefix) throws Exception {
        validateObjectPrefix(prefix);

        Iterable<Result<Item>> results = listObjects(prefix);
        for (Result<Item> result : results) {
            Item item = result.get();
            deleteFile(item.objectName());
        }
    }

    /**
     * 删除实验的所有资源 - 从resource bucket中删除
     * 
     * @param experimentId 实验ID
     * @throws Exception 如果删除失败
     */
    public void deleteExperimentResources(String experimentId) throws Exception {
        deleteByPrefixInBucket(RESOURCE_BUCKET, experimentId + "/");
    }

    /**
     * 删除实验的学习资料
     * 
     * @param experimentId 实验ID
     * @throws Exception 如果删除失败
     */
    public void deleteExperimentLearningResources(String experimentId) throws Exception {
        deleteByPrefixInBucket(RESOURCE_BUCKET, experimentId + "/resource/");
    }

    /**
     * 删除实验的源代码和评测脚本
     * 
     * @param experimentId 实验ID
     * @throws Exception 如果删除失败
     */
    public void deleteExperimentCodeResources(String experimentId) throws Exception {
        deleteByPrefixInBucket(RESOURCE_BUCKET, experimentId + "/experiment/");
    }

    /**
     * 删除学生对特定实验的所有提交 - 从submission bucket中删除
     * 
     * @param studentId    学生ID
     * @param experimentId 实验ID
     * @throws Exception 如果删除失败
     */
    public void deleteStudentSubmissions(String studentId, String experimentId) throws Exception {
        deleteByPrefixInBucket(SUBMISSION_BUCKET, studentId + "/" + experimentId + "/");
    }

    /**
     * 删除学生对特定任务的所有提交
     * 
     * @param studentId    学生ID
     * @param experimentId 实验ID
     * @param taskId       任务ID
     * @throws Exception 如果删除失败
     */
    public void deleteStudentTaskSubmissions(String studentId, String experimentId, String taskId) throws Exception {
        deleteByPrefixInBucket(SUBMISSION_BUCKET, studentId + "/" + experimentId + "/" + taskId + "/");
    }

    /**
     * 删除指定bucket中特定前缀下的所有文件
     * 
     * @param bucketName bucket名称
     * @param prefix     前缀路径
     * @throws Exception 如果删除失败
     */
    private void deleteByPrefixInBucket(String bucketName, String prefix) throws Exception {
        Iterable<Result<Item>> results = listObjects(bucketName, prefix);
        for (Result<Item> result : results) {
            Item item = result.get();
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(item.objectName())
                    .build());
        }
    }

    /**
     * 生成文件的临时预览URL
     * 
     * @param objectName MinIO中的对象名
     * @param expiryTime URL过期时间(秒)
     * @return 临时访问URL
     * @throws Exception 如果生成URL失败
     */
    public String generatePreviewUrl(String objectName, int expiryTime) throws Exception {
    // 验证对象路径前缀合法性
    validateObjectPrefix(objectName);

    // 根据对象路径确定使用哪个bucket
    String bucketName = determineBucketByObjectPath(objectName);

    // 构建拼接式URL: http://your-minio-server/bucket/object-path
    String baseUrl = minioConfig.getEndpoint(); // 需要确保MinioConfig中有getEndpoint()方法
    
    // 移除末尾的斜杠（如果有）
    if (baseUrl.endsWith("/")) {
        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    
    // 构建完整的URL
    String fullUrl = String.format("%s/%s/%s", baseUrl, bucketName, objectName);
    
    return fullUrl;
}

    /**
     * 验证对象前缀合法性
     * 可以根据用户角色/权限进行验证
     * 
     * @param objectName 对象路径
     * @throws IllegalArgumentException 如果访问无权限的前缀
     */
    private void validateObjectPrefix(String objectName) {
        validateObjectPath(objectName);
        // TODO: 实现基于用户角色的权限校验
        // 例如：验证当前用户是否有权限访问该路径
    }

    /**
     * 验证对象路径的基本合法性
     * 
     * @param objectName 对象路径
     * @throws IllegalArgumentException 如果路径格式非法
     */
    private void validateObjectPath(String objectName) {
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new IllegalArgumentException("对象路径不能为空");
        }

        if (objectName.contains("../") || objectName.contains("..\\")) {
            throw new IllegalArgumentException("非法的对象路径：不允许使用相对路径");
        }

        if (objectName.startsWith("/") || objectName.startsWith("\\")) {
            throw new IllegalArgumentException("非法的对象路径：不允许以分隔符开头");
        }
    }

    /**
     * 获取MinIO配置的桶名
     * 
     * @return 桶名
     */
    public String getBucketName() {
        return minioConfig.getBucketName();
    }

    /**
     * 上传实验资源 - 现在将根据资源类型存储在resource bucket的不同路径中
     * 学习资料: {experimentId}/resource/
     * 源代码和评测脚本: {experimentId}/experiment/
     * 
     * @param experimentId 实验ID
     * @param filename     文件名称
     * @param inputStream  文件输入流
     * @param size         文件大小
     * @param contentType  文件类型
     * @param resourceType 资源类型("resource"表示学习资料，"experiment"表示源代码和评测脚本)
     * @return 对象存储路径
     * @throws Exception 如果上传失败
     */
    public String uploadExperimentResource(String experimentId, String filename, InputStream inputStream,
            long size, String contentType, String resourceType) throws Exception {
        // 根据资源类型确定子目录，默认为"resource"
        String subDirectory = RESOURCE_TYPE_EXPERIMENT.equals(resourceType) ? RESOURCE_TYPE_EXPERIMENT
                : RESOURCE_TYPE_LEARNING;
        String objectName = generateExperimentResourcePath(experimentId, subDirectory, filename);
        return uploadToResourceBucket(objectName, inputStream, size, contentType);
    }

    /**
     * 上传学生提交 - 现在将存储在submission bucket的{studentId}/{experimentId}/{taskId}路径中
     * 
     * @param studentId    学生ID
     * @param experimentId 实验ID
     * @param taskId       任务ID
     * @param filename     文件名称
     * @param inputStream  文件输入流
     * @param size         文件大小
     * @param contentType  文件类型
     * @return 对象存储路径
     * @throws Exception 如果上传失败
     */    public String uploadStudentSubmission(String studentId, String experimentId, String taskId,
            String filename, InputStream inputStream,
            long size, String contentType) throws Exception {
        String objectName = generateStudentSubmissionPath(studentId, experimentId, taskId, filename);
        return uploadToSubmissionBucket(objectName, inputStream, size, contentType);
    }    /**
     * 上传学生代码提交（JSON格式） - 将多个源代码文件上传到submission bucket
     * 
     * @param studentId    学生ID
     * @param submissionRequest 提交请求DTO（包含experimentId、taskId和files）
     * @return 上传的文件路径列表
     * @throws Exception 如果上传失败
     */
    public List<String> uploadStudentCodeSubmission(String studentId, SubmissionRequestDTO submissionRequest) throws Exception {
        if (submissionRequest == null || submissionRequest.getFiles() == null || submissionRequest.getFiles().isEmpty()) {
            throw new IllegalArgumentException("代码提交内容不能为空");
        }
        
        String experimentId = submissionRequest.getExperimentId();
        String taskId = submissionRequest.getTaskId();
        List<SourceCodeFileDTO> files = submissionRequest.getFiles();
        
        // 生成时间戳目录
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
        List<String> uploadedPaths = new ArrayList<>();
        
        for (SourceCodeFileDTO file : files) {
            if (file.getFileName() == null || file.getContent() == null) {
                System.err.println("跳过无效文件: fileName=" + file.getFileName());
                continue;
            }
            
            try {
                // 生成对象路径: {studentId}/{experimentId}/{taskId}/{timestamp}/{fileName}
                String objectName = String.format("%s%s%s%s%s%s%s%s%s", 
                    studentId, PATH_SEPARATOR, 
                    experimentId, PATH_SEPARATOR, 
                    taskId, PATH_SEPARATOR, 
                    timestamp, PATH_SEPARATOR, 
                    file.getFileName());
                
                // 将文件内容转换为输入流
                byte[] contentBytes = file.getContent().getBytes("UTF-8");
                InputStream contentStream = new ByteArrayInputStream(contentBytes);
                
                // 上传文件到submission bucket
                String uploadedPath = uploadToSubmissionBucket(objectName, contentStream, 
                    contentBytes.length, "text/plain");
                uploadedPaths.add(uploadedPath);
                
                System.out.println("成功上传文件: " + objectName);
                
            } catch (Exception e) {
                System.err.println("上传文件失败: " + file.getFileName() + ", 错误: " + e.getMessage());
                throw new Exception("上传文件失败: " + file.getFileName(), e);
            }
        }
        
        return uploadedPaths;
    }

    /**
     * 上传通用资源到默认bucket
     * 
     * @param resourceType 资源类型
     * @param filename     文件名称
     * @param inputStream  文件输入流
     * @param size         文件大小
     * @param contentType  文件类型
     * @return 对象存储路径
     * @throws Exception 如果上传失败
     */
    public String uploadResource(String resourceType, String filename, InputStream inputStream,
            long size, String contentType) throws Exception {
        String objectName = "resources/" + resourceType + "/" + UUID.randomUUID().toString() +
                "_" + filename;
        return uploadObject(objectName, inputStream, size, contentType);
    }

    /**
     * 基础上传方法 - 上传到指定bucket
     *
     * @param bucketName  bucket名称
     * @param objectName  对象名称
     * @param inputStream 文件输入流
     * @param size        文件大小
     * @param contentType 文件类型
     * @return 对象存储路径
     * @throws Exception 如果上传失败
     */
    private String uploadToBucket(String bucketName, String objectName, InputStream inputStream,
            long size, String contentType) throws Exception {
        ensureBucketExists();
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build());

        return objectName;
    }

    /**
     * 基础上传方法 - 上传到默认bucket
     *
     * @param objectName  对象名称
     * @param inputStream 文件输入流
     * @param size        文件大小
     * @param contentType 文件类型
     * @return 对象存储路径
     * @throws Exception 如果上传失败
     */
    private String uploadObject(String objectName, InputStream inputStream,
            long size, String contentType) throws Exception {
        return uploadToBucket(minioConfig.getBucketName(), objectName, inputStream, size, contentType);
    }

    /**
     * 根据对象路径确定应该使用的bucket
     * 
     * @param objectName 对象路径
     * @return bucket名称
     */
    private String determineBucketByObjectPath(String objectName) {
        if (objectName == null) {
            return minioConfig.getBucketName();
        }
        // 头像文件应该存储在默认bucket中
        if (objectName.startsWith(PREFIX_AVATARS)) {
            return minioConfig.getBucketName();
        }
        // 判断是否为实验资源路径格式：{experimentId}/experiment/...
        if (isExperimentResourcePath(objectName)) {
            return RESOURCE_BUCKET;
        }

        // 判断是否为学生提交路径格式：{studentId}/{experimentId}/{taskId}/...
        if (isStudentSubmissionPath(objectName)) {
            return SUBMISSION_BUCKET;
        }

        // 其他情况使用默认bucket
        return minioConfig.getBucketName();
    }

    /**
     * 判断是否为学生提交路径格式
     * 路径格式：{studentId}/{experimentId}/{taskId}/...
     * 
     * @param objectName 对象路径
     * @return 是否为学生提交路径
     */
    private boolean isStudentSubmissionPath(String objectName) {
        if (objectName == null) {
            return false;
        }

        String[] parts = objectName.split(PATH_SEPARATOR);
        // 至少需要3个部分：studentId/experimentId/taskId
        return parts.length >= 3;
    }

    /**
     * 判断是否为实验资源路径格式
     * 路径格式：{experimentId}/resource/... 或 {experimentId}/experiment/...
     * 
     * @param objectName 对象路径
     * @return 是否为实验资源路径
     */
    private boolean isExperimentResourcePath(String objectName) {
        if (objectName == null) {
            return false;
        }

        String[] parts = objectName.split(PATH_SEPARATOR);
        if (parts.length < 2) {
            return false;
        }

        // 检查第二部分是否为 "resource" 或 "experiment"
        String secondPart = parts[1];
        return RESOURCE_TYPE_LEARNING.equals(secondPart) || RESOURCE_TYPE_EXPERIMENT.equals(secondPart);
    }

    /**
     * 上传文件到submission bucket
     *
     * @param objectName  对象名称
     * @param inputStream 文件输入流
     * @param size        文件大小
     * @param contentType 文件类型
     * @return 对象存储路径
     * @throws Exception 如果上传失败
     */
    private String uploadToSubmissionBucket(String objectName, InputStream inputStream,
            long size, String contentType) throws Exception {
        return uploadToBucket(SUBMISSION_BUCKET, objectName, inputStream, size, contentType);
    }

    /**
     * 上传文件到resource bucket
     *
     * @param objectName  对象名称
     * @param inputStream 文件输入流
     * @param size        文件大小
     * @param contentType 文件类型
     * @return 对象存储路径
     * @throws Exception 如果上传失败
     */
    private String uploadToResourceBucket(String objectName, InputStream inputStream,
            long size, String contentType) throws Exception {
        return uploadToBucket(RESOURCE_BUCKET, objectName, inputStream, size, contentType);
    }

    /**
     * 判断对象是否为实验资源 - 已废弃，新架构中实验资源存储在resource bucket中
     * 
     * @param objectName 对象名称
     * @return 是否为实验资源
     * @deprecated 使用新的bucket架构，此方法已不适用
     */
    @Deprecated
    public boolean isExperimentResource(String objectName) {
        return objectName != null && objectName.startsWith("resources/experiments/");
    }

    /**
     * 判断对象是否为学生提交 - 已废弃，新架构中学生提交存储在submission bucket中
     * 
     * @param objectName 对象名称
     * @return 是否为学生提交
     * @deprecated 使用新的bucket架构，此方法已不适用
     */
    @Deprecated
    public boolean isStudentSubmission(String objectName) {
        return objectName != null && objectName.startsWith("submissions/");
    }

    /**
     * 获取对象的学生ID
     * 适用于学生提交路径格式：submissions/{studentId}/...
     * 
     * @param objectName 对象名称
     * @return 学生ID，如果不是学生提交则返回null
     */
    public String getStudentIdFromPath(String objectName) {
        if (!isStudentSubmission(objectName)) {
            return null;
        }

        String[] parts = objectName.split("/");
        if (parts.length < 3) {
            return null;
        }

        return parts[1]; // submissions/{studentId}/...
    }

    /**
     * 获取对象的实验ID
     * 
     * @param objectName 对象名称
     * @return 实验ID，如果无法解析则返回null
     */
    public String getExperimentIdFromPath(String objectName) {
        if (objectName == null) {
            return null;
        }

        String[] parts = objectName.split("/");

        if (isExperimentResource(objectName) && parts.length >= 4) {
            return parts[2]; // resources/experiments/{experimentId}/...
        } else if (isStudentSubmission(objectName) && parts.length >= 4) {
            return parts[2]; // submissions/{studentId}/{experimentId}/...
        }

        return null;
    }

    /**
     * 列出指定前缀下的所有对象 - 根据前缀自动选择bucket
     * 
     * @param prefix 前缀路径
     * @return 对象名称列表
     * @throws Exception 如果操作失败
     */
    public Iterable<Result<Item>> listObjects(String prefix) throws Exception {
        String bucketName = determineBucketByObjectPath(prefix);
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(true)
                        .build());
    }

    /**
     * 列出指定bucket和前缀下的所有对象
     * 
     * @param bucketName bucket名称
     * @param prefix     前缀路径
     * @return 对象名称列表
     * @throws Exception 如果操作失败
     */
    public Iterable<Result<Item>> listObjects(String bucketName, String prefix) throws Exception {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(true)
                        .build());
    }

    /**
     * 列出所有实验资源 - 从resource bucket中列出
     * 
     * @return 实验资源对象列表
     * @throws Exception 如果操作失败
     */
    public Iterable<Result<Item>> listExperimentResources() throws Exception {
        return listObjects(RESOURCE_BUCKET, "");
    }

    /**
     * 列出特定实验的资源 - 从resource bucket中列出
     * 
     * @param experimentId 实验ID
     * @return 该实验的资源对象列表
     * @throws Exception 如果操作失败
     */
    public Iterable<Result<Item>> listExperimentResources(String experimentId) throws Exception {
        return listObjects(RESOURCE_BUCKET, experimentId + "/");
    }

    /**
     * 列出特定实验的学习资料
     * 
     * @param experimentId 实验ID
     * @return 该实验的学习资料对象列表
     * @throws Exception 如果操作失败
     */
    public Iterable<Result<Item>> listExperimentLearningResources(String experimentId) throws Exception {
        return listObjects(RESOURCE_BUCKET, experimentId + "/resource/");
    }

    /**
     * 列出特定实验的源代码和评测脚本
     * 
     * @param experimentId 实验ID
     * @return 该实验的源代码和评测脚本对象列表
     * @throws Exception 如果操作失败
     */
    public Iterable<Result<Item>> listExperimentCodeResources(String experimentId) throws Exception {
        return listObjects(RESOURCE_BUCKET, experimentId + "/experiment/");
    }

    /**
     * 列出学生的所有提交 - 从submission bucket中列出
     * 
     * @param studentId 学生ID
     * @return 该学生的提交对象列表
     * @throws Exception 如果操作失败
     */
    public Iterable<Result<Item>> listStudentSubmissions(String studentId) throws Exception {
        return listObjects(SUBMISSION_BUCKET, studentId + "/");
    }

    /**
     * 列出学生对特定实验的所有提交 - 从submission bucket中列出
     * 
     * @param studentId    学生ID
     * @param experimentId 实验ID
     * @return 该学生对该实验的提交对象列表
     * @throws Exception 如果操作失败
     */
    public Iterable<Result<Item>> listStudentSubmissionsForExperiment(String studentId, String experimentId)
            throws Exception {
        return listObjects(SUBMISSION_BUCKET, studentId + "/" + experimentId + "/");
    }

    /**
     * 列出学生对特定任务的所有提交
     * 
     * @param studentId    学生ID
     * @param experimentId 实验ID
     * @param taskId       任务ID
     * @return 该学生对该任务的提交对象列表
     * @throws Exception 如果操作失败
     */
    public Iterable<Result<Item>> listStudentSubmissionsForTask(String studentId, String experimentId, String taskId)
            throws Exception {
        return listObjects(SUBMISSION_BUCKET, studentId + "/" + experimentId + "/" + taskId + "/");
    }

    /**
     * 检查文件是否存在 - 根据对象路径自动选择bucket
     * 
     * @param objectName 对象名称
     * @return 是否存在
     */
    public boolean isFileExists(String objectName) {
        try {
            String bucketName = determineBucketByObjectPath(objectName);
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取文件元数据 - 根据对象路径自动选择bucket
     * 
     * @param objectName 对象名称
     * @return 文件元数据
     * @throws Exception 如果操作失败
     */
    public StatObjectResponse getFileMetadata(String objectName) throws Exception {
        validateObjectPrefix(objectName);

        String bucketName = determineBucketByObjectPath(objectName);
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
    }

    /**
     * 获取常量 - 适配新的bucket结构
     */
    public static String getSubmissionBucket() {
        return SUBMISSION_BUCKET;
    }

    public static String getResourceBucket() {
        return RESOURCE_BUCKET;
    }

    /**
     * 获取资源前缀常量 - 保留向后兼容性
     * 
     * @deprecated 建议使用新的bucket架构
     */
    @Deprecated
    public static String getExperimentsPrefix() {
        return "resources/experiments/";
    }

    @Deprecated
    public static String getSubmissionsPrefix() {
        return "submissions/";
    }

    @Deprecated
    public static String getResourcesPrefix() {
        return "resources/";
    }

    /**
     * 上传用户头像
     * 
     * @param file   要上传的头像文件
     * @param userId 用户ID
     * @return 头像在MinIO中的存储路径
     * @throws Exception 如果上传失败
     */
    public String uploadUserAvatar(MultipartFile file, String userId) throws Exception {
        String objectName = generateUserAvatarPath(userId, file.getOriginalFilename());
        return uploadObject(objectName, file.getInputStream(), file.getSize(), file.getContentType());
    }

    /**
     * 删除用户原有头像
     * 
     * @param avatarPath 旧头像路径
     * @throws Exception 如果删除失败
     */
    public void deleteUserAvatar(String avatarPath) throws Exception {
        if (avatarPath != null && avatarPath.startsWith(PREFIX_AVATARS)) {
            deleteFile(avatarPath);
        }
    }

    /**
     * 获取用户头像的预览URL
     * 
     * @param avatarPath 头像在MinIO中的存储路径
     * @param expiryTime URL过期时间(秒)
     * @return 临时访问URL
     * @throws Exception 如果生成URL失败
     */
    public String getAvatarPreviewUrl(String avatarPath, int expiryTime) throws Exception {
        if (avatarPath == null || !avatarPath.startsWith(PREFIX_AVATARS)) {
            throw new IllegalArgumentException("非法的头像路径");
        }

        return generatePreviewUrl(avatarPath, expiryTime);
    }

    /**
     * 生成带UUID的文件名
     * 
     * @param filename 原始文件名
     * @return 带UUID的文件名
     */
    private String generateUniqueFileName(String filename) {
        return UUID.randomUUID().toString() + "_" + filename;
    }

    /**
     * 生成带时间戳的文件名
     * 
     * @param filename 原始文件名
     * @return 带时间戳的文件名
     */
    private String generateTimestampFileName(String filename) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
        return timestamp + "-" + filename;
    }

    /**
     * 生成实验资源路径
     * 
     * @param experimentId 实验ID
     * @param resourceType 资源类型("resource"或"experiment")
     * @param filename     文件名
     * @return 完整的对象路径
     */
    private String generateExperimentResourcePath(String experimentId, String resourceType, String filename) {
        return String.format("%s%s%s%s%s", experimentId, PATH_SEPARATOR, resourceType, PATH_SEPARATOR,
                generateUniqueFileName(filename));
    }

    /**
     * 生成学生提交路径
     * 
     * @param studentId    学生ID
     * @param experimentId 实验ID
     * @param taskId       任务ID
     * @param filename     文件名
     * @return 完整的对象路径
     */
    private String generateStudentSubmissionPath(String studentId, String experimentId, String taskId,
            String filename) {
        return String.format("%s%s%s%s%s%s%s", studentId, PATH_SEPARATOR, experimentId, PATH_SEPARATOR, taskId,
                PATH_SEPARATOR, generateTimestampFileName(filename));
    }

    /**
     * 生成用户头像路径
     * 
     * @param userId   用户ID
     * @param filename 文件名
     * @return 完整的对象路径
     */
    private String generateUserAvatarPath(String userId, String filename) {
        // 获取文件扩展名
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf("."));
        }        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
        return PREFIX_AVATARS + userId + PATH_SEPARATOR + timestamp + "_" + UUID.randomUUID().toString() + extension;
    }

    /**
     * 检测文件是否为压缩包
     * 
     * @param filename 文件名
     * @return 是否为压缩包
     */
    public boolean isCompressedFile(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".zip") || lowerFilename.endsWith(".rar") || 
               lowerFilename.endsWith(".7z") || lowerFilename.endsWith(".tar") || 
               lowerFilename.endsWith(".gz") || lowerFilename.endsWith(".tar.gz");
    }    /**
     * 自动解压压缩包并上传所有文件，保持目录结构
     * 
     * @param experimentId 实验ID
     * @param taskId 任务ID（可选）
     * @param compressedFile 压缩文件流
     * @param filename 压缩文件名
     * @param contentType 压缩文件类型
     * @return 解压出的文件路径列表
     * @throws Exception 如果解压或上传失败
     */
    public List<String> extractAndUploadCompressedFile(String experimentId, String taskId, 
            InputStream compressedFile, String filename, String contentType) throws Exception {
        
        List<String> uploadedFiles = new ArrayList<>();
        
        // 首先读取原始压缩文件内容
        byte[] compressedData = readAllBytes(compressedFile);
        
        // 生成基础路径
        String basePath = generateExperimentResourceBasePath(experimentId, taskId);
        
        // 根据文件扩展名确定解压方式
        String lowerFilename = filename.toLowerCase();
        
        try {
            if (lowerFilename.endsWith(".zip")) {
                uploadedFiles.addAll(extractZipFile(new ByteArrayInputStream(compressedData), basePath));
            } else if (lowerFilename.endsWith(".tar") || lowerFilename.endsWith(".tar.gz")) {
                uploadedFiles.addAll(extractTarFile(new ByteArrayInputStream(compressedData), basePath, lowerFilename.endsWith(".gz")));
            } else if (lowerFilename.endsWith(".gz") && !lowerFilename.endsWith(".tar.gz")) {
                uploadedFiles.addAll(extractGzFile(new ByteArrayInputStream(compressedData), basePath, filename));
            } else {
                throw new UnsupportedOperationException("不支持的压缩格式: " + filename);
            }
            
            // 同时保存原始压缩包
            String compressedFilePath = uploadOriginalCompressedFile(experimentId, taskId, 
                    new ByteArrayInputStream(compressedData), filename, contentType, compressedData.length);
            uploadedFiles.add(compressedFilePath);
            
        } catch (Exception e) {
            throw new Exception("解压文件失败: " + e.getMessage(), e);
        }
        
        return uploadedFiles;
    }

    /**
     * 解压ZIP文件
     */
    private List<String> extractZipFile(InputStream zipStream, String basePath) throws Exception {
        List<String> uploadedFiles = new ArrayList<>();
          try (ZipArchiveInputStream zis = new ZipArchiveInputStream(zipStream)) {
            ZipArchiveEntry entry;
            while ((entry = (ZipArchiveEntry) zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String entryPath = basePath + entry.getName();
                    
                    // 读取文件内容
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    
                    // 上传文件到MinIO
                    InputStream fileStream = new ByteArrayInputStream(baos.toByteArray());
                    String mimeType = detectMimeTypeFromFilename(entry.getName());
                    String uploadedPath = uploadToResourceBucket(entryPath, fileStream, 
                            baos.size(), mimeType);
                    
                    // 添加元数据记录目录结构
                    addDirectoryMetadata(uploadedPath, entry.getName(), "extracted_from_zip");
                    
                    uploadedFiles.add(uploadedPath);
                }
            }
        }
        
        return uploadedFiles;
    }

    /**
     * 解压TAR文件
     */
    private List<String> extractTarFile(InputStream tarStream, String basePath, boolean isGzipped) throws Exception {
        List<String> uploadedFiles = new ArrayList<>();
        
        InputStream inputStream = isGzipped ? new GzipCompressorInputStream(tarStream) : tarStream;
          try (TarArchiveInputStream tis = new TarArchiveInputStream(inputStream)) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String entryPath = basePath + entry.getName();
                    
                    // 读取文件内容
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = tis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    
                    // 上传文件到MinIO
                    InputStream fileStream = new ByteArrayInputStream(baos.toByteArray());
                    String mimeType = detectMimeTypeFromFilename(entry.getName());
                    String uploadedPath = uploadToResourceBucket(entryPath, fileStream, 
                            baos.size(), mimeType);
                    
                    // 添加元数据记录目录结构
                    addDirectoryMetadata(uploadedPath, entry.getName(), "extracted_from_tar");
                    
                    uploadedFiles.add(uploadedPath);
                }
            }
        }
        
        return uploadedFiles;
    }

    /**
     * 解压GZ文件（单文件压缩）
     */
    private List<String> extractGzFile(InputStream gzStream, String basePath, String originalFilename) throws Exception {
        List<String> uploadedFiles = new ArrayList<>();
        
        // 移除.gz扩展名得到原始文件名
        String extractedFilename = originalFilename.substring(0, originalFilename.lastIndexOf(".gz"));
        String entryPath = basePath + extractedFilename;
        
        try (GzipCompressorInputStream gis = new GzipCompressorInputStream(gzStream)) {
            // 读取解压后的内容
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            
            // 上传文件到MinIO
            InputStream fileStream = new ByteArrayInputStream(baos.toByteArray());
            String mimeType = detectMimeTypeFromFilename(extractedFilename);
            String uploadedPath = uploadToResourceBucket(entryPath, fileStream, 
                    baos.size(), mimeType);
            
            // 添加元数据记录目录结构
            addDirectoryMetadata(uploadedPath, extractedFilename, "extracted_from_gz");
            
            uploadedFiles.add(uploadedPath);
        }
        
        return uploadedFiles;
    }    /**
     * 上传原始压缩文件
     */
    private String uploadOriginalCompressedFile(String experimentId, String taskId, 
            InputStream compressedFile, String filename, String contentType, long fileSize) throws Exception {
        String basePath = generateExperimentResourceBasePath(experimentId, taskId);
        String compressedFilePath = basePath + "original/" + generateUniqueFileName(filename);
        
        return uploadToResourceBucket(compressedFilePath, compressedFile, fileSize, contentType);
    }

    /**
     * 生成实验资源基础路径
     */
    private String generateExperimentResourceBasePath(String experimentId, String taskId) {
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(experimentId).append(PATH_SEPARATOR);
        pathBuilder.append(RESOURCE_TYPE_EXPERIMENT).append(PATH_SEPARATOR);
        
        if (taskId != null && !taskId.trim().isEmpty()) {
            pathBuilder.append(taskId).append(PATH_SEPARATOR);
        }
        
        return pathBuilder.toString();
    }    /**
     * 通过MinIO元数据添加目录结构信息
     */
    private void addDirectoryMetadata(String objectPath, String originalPath, String extractionType) throws Exception {
        // 创建元数据映射
        Map<String, String> metadata = new HashMap<>();
        metadata.put("original-path", originalPath);
        metadata.put("extraction-type", extractionType);
        metadata.put("extraction-time", LocalDateTime.now().toString());
        
        // 注意：MinIO不支持直接更新已上传文件的元数据
        // 这里只是记录，在实际应用中可以考虑在上传时直接设置元数据
        // 或者使用数据库记录这些信息
    }

    /**
     * 根据文件名检测MIME类型
     */
    private String detectMimeTypeFromFilename(String filename) {
        String extension = "";
        if (filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        }
        
        switch (extension) {
            case ".java": return "text/x-java-source";
            case ".cpp": case ".c": return "text/x-c";
            case ".py": return "text/x-python";
            case ".js": return "application/javascript";
            case ".html": return "text/html";
            case ".css": return "text/css";
            case ".txt": return "text/plain";
            case ".md": return "text/markdown";
            case ".json": return "application/json";
            case ".xml": return "application/xml";
            default: return "application/octet-stream";
        }
    }

    /**
     * 读取输入流的所有字节
     */
    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len;
        while ((len = inputStream.read(data)) != -1) {
            buffer.write(data, 0, len);
        }
        return buffer.toByteArray();
    }
}
