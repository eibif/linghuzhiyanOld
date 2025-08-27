package org.linghu.test;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;

public class MinioTest {
    @Test
    public  void minioTest() {
        // 请根据实际情况填写 MinIO 服务地址、AccessKey 和 SecretKey
        String endpoint = "http://10.128.54.190:9000";
        String accessKey = "keyan";
        String secretKey = "lop*123456lop*";

        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        try {
            boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket("linghuzhiyan").build());
            System.out.println("MinIO连接成功，存储桶存在: " + bucketExists);
        } catch (Exception e) {
            System.err.println("MinIO连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
