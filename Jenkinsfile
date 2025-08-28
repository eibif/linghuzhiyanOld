pipeline {
    agent any
    environment {
        IMAGE_NAME = "linghuzhiyan-app"
        DOCKER_IMAGE = "${IMAGE_NAME}:${BUILD_NUMBER}"
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
                            sh "docker build -t ${DOCKER_IMAGE} ."
                        } else {
                            bat "docker build -t ${DOCKER_IMAGE} ."
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
                                sh 'kubectl apply -f app.yaml'
                            } else {
                                bat 'kubectl apply -f namespace.yaml'
                                bat 'kubectl apply -f secrets.yaml'
                                bat 'kubectl apply -f mysql.yaml'
                                bat 'kubectl apply -f mongo.yaml'
                                bat 'kubectl apply -f minio.yaml'
                                bat 'kubectl apply -f app.yaml'
                            }
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
