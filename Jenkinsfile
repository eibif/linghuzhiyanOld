pipeline {
    agent any
    environment {
        IMAGE_NAME = "linghuzhiyan-app"
        KUBE_CONFIG = credentials('kubeconfig') // Jenkins凭据ID
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build Maven Project') {
            steps {
                dir('softengineer/linghuzhiyan') {
                    script {
                        if (isUnix()) {
                            sh 'chmod +x mvnw || echo skip'
                            sh './mvnw clean package -DskipTests'
                        } else {
                            bat 'mvnw.cmd clean package -DskipTests'
                        }
                    }
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                dir('softengineer/linghuzhiyan') {
                    script {
                        if (isUnix()) {
                            sh "docker build -t ${IMAGE_NAME}:latest ."
                        } else {
                            bat "docker build -t ${IMAGE_NAME}:latest ."
                        }
                    }
                }
            }
        }
        stage('Run Docker Compose') {
            steps {
                dir('softengineer/linghuzhiyan') {
                    script {
                        if (isUnix()) {
                            sh "docker compose up -d"
                        } else {
                            bat "docker compose up -d"
                        }
                    }
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    dir('softengineer/linghuzhiyan/deploy/k8s') {
                        script {
                            if (isUnix()) {
                                sh 'kubectl apply -f namespace.yaml'
                                sh 'kubectl apply -f secrets.yaml'
                                sh 'kubectl apply -f mysql.yaml'
                                sh 'kubectl apply -f mongo.yaml'
                                sh 'kubectl apply -f minio.yaml'
                                sh 'kubectl apply -f go-judge.yaml'
                                sh 'kubectl apply -f app.yaml'
                            } else {
                                bat 'kubectl apply -f namespace.yaml'
                                bat 'kubectl apply -f secrets.yaml'
                                bat 'kubectl apply -f mysql.yaml'
                                bat 'kubectl apply -f mongo.yaml'
                                bat 'kubectl apply -f minio.yaml'
                                bat 'kubectl apply -f go-judge.yaml'
                                bat 'kubectl apply -f app.yaml'
                            }
                        }
                    }
                }
            }
        }
        stage('Health Check') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    script {
                        if (isUnix()) {
                            sh '''
                                echo "等待go-judge服务启动..."
                                kubectl wait --for=condition=ready pod -l app=go-judge -n linghu --timeout=300s
                                echo "等待应用服务启动..."
                                kubectl wait --for=condition=ready pod -l app=linghuzhiyan -n linghu --timeout=300s
                                echo "检查服务状态..."
                                kubectl get pods -n linghu
                                kubectl get services -n linghu
                            '''
                        } else {
                            bat '''
                                echo 等待go-judge服务启动...
                                kubectl wait --for=condition=ready pod -l app=go-judge -n linghu --timeout=30s
                                echo 等待应用服务启动...
                                kubectl wait --for=condition=ready pod -l app=linghuzhiyan -n linghu --timeout=30s
                                echo 检查服务状态...
                                kubectl get pods -n linghu
                                kubectl get services -n linghu
                            '''
                        }
                    }
                }
            }
        }
    }
    post {
        failure {
            echo 'Pipeline failed.'
        }
        success {
            echo 'Pipeline succeeded.'
        }
    }
}
