# go-judge 评测服务部署说明

## 概述
go-judge 是一个高性能的代码评测服务，用于在线判题和代码执行。本文档说明了在 Kubernetes 环境中部署 go-judge 的配置和使用。

## 架构说明
- **go-judge 服务**: 提供代码评测功能，支持多种编程语言
- **端口配置**:
  - 5050: HTTP API 端口
  - 5051: gRPC API 端口  
  - 5052: 监控指标端口

## Kubernetes 部署

### 服务配置
- **命名空间**: linghu
- **镜像**: criyle/go-judge:latest
- **安全配置**: 
  - 特权模式运行 (privileged: true)
  - 添加 SYS_ADMIN 和 SYS_PTRACE 权限
  - 自定义 seccomp 配置文件

### 资源限制
- **内存**: 请求 512Mi，限制 2Gi
- **CPU**: 请求 250m，限制 1000m
- **临时存储**: 1Gi
- **共享内存**: 256Mi

### 健康检查
- **就绪检查**: GET /version (端口 5050)
- **存活检查**: GET /version (端口 5050)

## 应用集成

### 环境变量配置
在应用中配置以下环境变量来连接 go-judge 服务：

```bash
JUDGE_SERVICE_URL=http://go-judge:5050
JUDGE_SERVICE_GRPC_URL=go-judge:5051
JUDGE_SERVICE_TIMEOUT=30
JUDGE_SERVICE_MAX_RETRIES=3
```

### Spring Boot 配置
在 `application-docker.properties` 中配置：

```properties
judge.service.url=${JUDGE_SERVICE_URL:http://go-judge:5050}
judge.service.grpc.url=${JUDGE_SERVICE_GRPC_URL:go-judge:5051}
judge.service.timeout=${JUDGE_SERVICE_TIMEOUT:30}
judge.service.maxRetries=${JUDGE_SERVICE_MAX_RETRIES:3}
```

## Jenkins 流水线
Jenkins 流水线自动部署 go-judge 服务：

1. 构建应用 Docker 镜像
2. 部署 Kubernetes 资源（包括 go-judge）
3. 健康检查和状态验证

### 部署顺序
1. namespace.yaml
2. secrets.yaml
3. mysql.yaml
4. mongo.yaml
5. minio.yaml
6. **go-judge.yaml** (评测服务)
7. app.yaml (主应用)

## API 使用示例

### 代码执行请求
```json
{
  "cmd": [{
    "args": ["/usr/bin/g++", "main.cpp", "-o", "main"],
    "env": ["PATH=/usr/bin:/bin"],
    "files": {
      "main.cpp": {
        "content": "#include<iostream>\nint main(){std::cout<<\"Hello World\";return 0;}"
      }
    },
    "cpuLimit": 1000000000,
    "memoryLimit": 268435456,
    "procLimit": 50,
    "copyIn": {
      "main.cpp": {
        "content": "#include<iostream>\nint main(){std::cout<<\"Hello World\";return 0;}"
      }
    },
    "copyOut": ["stdout", "stderr"]
  }]
}
```

## 监控和日志
- 通过 5052 端口访问 Prometheus 监控指标
- 查看 Pod 日志: `kubectl logs -f deployment/go-judge -n linghu`
- 检查服务状态: `kubectl get pods -l app=go-judge -n linghu`

## 安全注意事项
1. go-judge 运行在特权模式下，具有系统级权限
2. 使用 seccomp 配置文件限制系统调用
3. 资源限制防止资源耗尽
4. 网络隔离确保服务安全

## 故障排除
1. **服务无法启动**: 检查特权模式和安全上下文配置
2. **内存不足**: 调整内存限制和共享内存大小
3. **网络连接问题**: 验证服务名称和端口配置
4. **权限问题**: 确认 seccomp 配置文件正确加载

## 支持的编程语言
- C/C++
- Java
- Python
- Go
- JavaScript (Node.js)
- 其他常见编程语言

更多详细信息请参考 [go-judge 官方文档](https://github.com/criyle/go-judge)。
