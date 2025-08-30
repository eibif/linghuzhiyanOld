package org.linghu.mybackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * go-judge评测服务配置
 */
@Configuration
@ConfigurationProperties(prefix = "judge.service")
public class JudgeConfig {
    
    /**
     * go-judge服务HTTP API地址
     */
    private String url = "http://localhost:5050";
    
    /**
     * go-judge服务gRPC地址
     */
    private Grpc grpc = new Grpc();
    
    /**
     * 请求超时时间（秒）
     */
    private int timeout = 30;
    
    /**
     * 最大重试次数
     */
    private int maxRetries = 3;
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public Grpc getGrpc() {
        return grpc;
    }
    
    public void setGrpc(Grpc grpc) {
        this.grpc = grpc;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    /**
     * gRPC相关配置
     */
    public static class Grpc {
        /**
         * gRPC服务地址
         */
        private String url = "localhost:5051";
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    /**
     * 获取运行代码的完整URL
     */
    public String getRunUrl() {
        return url + "/run";
    }
    
    /**
     * 获取文件上传的完整URL
     */
    public String getFileUrl() {
        return url + "/file";
    }
    
    /**
     * 获取版本信息的完整URL
     */
    public String getVersionUrl() {
        return url + "/version";
    }
}
