package org.linghu.mybackend.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置类
 * 用于配置MinIO客户端及相关属性
 */
@Data

@Configuration
public class MinioConfig {

    /**
     * MinIO服务地址
     */
    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 访问密钥
     */
    @Value("${minio.accessKey}")
    private String accessKey;

    /**
     * 密钥
     */
    @Value("${minio.secretKey}")
    private String secretKey;

    /**
     * 存储桶名称
     */
    @Value("${minio.bucketName}")
    private String bucketName;
    /**
     * 是否使用安全连接（HTTPS）
     */
    @Value("${minio.secure:false}")
    private boolean secure;

    /**
     * 创建MinIO客户端
     * 
     * @return MinioClient
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
