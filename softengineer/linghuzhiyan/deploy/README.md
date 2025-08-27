# 一键部署指南（Docker Desktop 与 Kubernetes）

本文档指导你使用 Docker Compose 本地一键启动，或使用 Docker Desktop 内置的 Kubernetes 进行集群一键部署。

## 1) 本地快速启动（Docker Compose）

前置要求：安装 Docker Desktop，并启用 Linux 容器。

步骤：
1. 在项目根的 `softengineer/linghuzhiyan` 目录构建镜像并启动服务：
   - `deploy/docker-compose.yml` 会启动：MySQL、MongoDB、MinIO 和应用。
2. 运行：
   ```powershell
   cd softengineer/linghuzhiyan/deploy
   docker compose up -d --build
   ```
3. 访问：
   - 应用：http://localhost:8080/swagger-ui.html
   - MinIO 控制台：http://localhost:9001 （默认账号密码：minioadmin/minioadmin）

> 应用默认使用 `application-docker.properties`（通过 `SPRING_PROFILES_ACTIVE=docker`）。

## 2) Kubernetes 一键部署（适用于 Docker Desktop 的 K8s 或任何 K8s 集群）

前置要求：
- 启用 Docker Desktop 的 Kubernetes（Settings -> Kubernetes -> Enable Kubernetes）。
- 安装 kubectl。
- 可选：安装 Ingress Controller（例如 NGINX Ingress）。

步骤：
1. 构建并推送镜像（这里建议推送到你可访问的镜像仓库，示例使用 ghcr.io）。
   ```powershell
   cd softengineer/linghuzhiyan
   docker build -t ghcr.io/<your-user>/linghuzhiyan:latest .
   docker push ghcr.io/<your-user>/linghuzhiyan:latest
   ```
   然后在 `deploy/k8s/app.yaml` 中把 `image` 改成你推送的镜像地址。

2. 应用所有清单：
   ```powershell
   cd deploy/k8s
   kubectl apply -f namespace.yaml
   kubectl apply -f secrets.yaml
   kubectl apply -f mysql.yaml
   kubectl apply -f mongo.yaml
   kubectl apply -f minio.yaml
   kubectl apply -f app.yaml
   ```

3. 检查状态：
   ```powershell
   kubectl get pods -n linghu
   kubectl get svc -n linghu
   ```

4. 暴露访问：
   - 如果安装了 Ingress Controller，并在 hosts 文件加入：
     ```
     127.0.0.1 linghu.local
     ```
     访问：http://linghu.local:80 或 http://linghu.local/swagger-ui.html
   - 若未安装 Ingress，可使用端口转发：
     ```powershell
     kubectl port-forward -n linghu svc/linghuzhiyan 8080:8080
     ```

## 配置说明
- 数据库：MySQL（库名 user_service），MongoDB（库名 discussion_service，root/example）。
- MinIO：默认凭据 minioadmin/minioadmin，bucketName 为 `linghuzhiyan`（请在首次运行后在 MinIO 控制台创建该 bucket，或在应用启动逻辑中自动创建）。
- JWT：通过 K8s Secret 或 Compose 环境变量注入。

## 常见问题
- Windows 上 3306/27017/9000/9001 端口被占用：修改 `docker-compose.yml` 的宿主映射端口或关闭占用程序。
- 镜像拉取失败：请确保 `app.yaml` 的镜像地址可访问，或把 `imagePullPolicy` 设为 `Never` 并使用本地镜像（仅适用于本地集群如 kind/minikube）。
- Ingress 404：确认已安装 NGINX Ingress，且 `kubernetes.io/ingress.class: nginx` 与实际安装的控制器匹配。
