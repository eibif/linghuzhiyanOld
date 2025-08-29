-- 清理现有表（注意外键依赖顺序）
DROP TABLE IF EXISTS experiment_evaluation;
DROP TABLE IF EXISTS experiment_submission;
DROP TABLE IF EXISTS experiment_assignment;
DROP TABLE IF EXISTS experiment_task;
DROP TABLE IF EXISTS resource;
DROP TABLE IF EXISTS experiment;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS users;

-- 创建题目表
CREATE TABLE question (
    id VARCHAR(36) PRIMARY KEY,
    question_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    options TEXT,
    answer TEXT,
    explanation TEXT,
    tags VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 创建用户表
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    avatar VARCHAR(255),
    profile VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- 创建实验表
CREATE TABLE experiment (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    creator_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT',
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 创建实验任务表
CREATE TABLE experiment_task (
    id VARCHAR(36) PRIMARY KEY,
    experiment_id VARCHAR(36) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    question_ids TEXT,
    required BOOLEAN DEFAULT TRUE,
    order_num INT DEFAULT 0,
    task_type VARCHAR(20) DEFAULT 'OTHER',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_task_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id) ON DELETE CASCADE
);

-- 创建实验任务分配表
CREATE TABLE experiment_assignment (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    UNIQUE (task_id, user_id),
    CONSTRAINT fk_assignment_task FOREIGN KEY (task_id) REFERENCES experiment_task(id) ON DELETE CASCADE
);

-- 创建实验提交表
CREATE TABLE experiment_submission (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    user_answer TEXT,
    score DECIMAL(5,2),
    grader_id VARCHAR(36),
    graded_time TIMESTAMP,
    time_spent INT,
    submit_time TIMESTAMP NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP,
    CONSTRAINT fk_submission_task FOREIGN KEY (task_id) REFERENCES experiment_task(id) ON DELETE CASCADE
);

-- 创建实验评测表
CREATE TABLE experiment_evaluation (
    id VARCHAR(36) PRIMARY KEY,
    submission_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    task_id VARCHAR(36) NOT NULL,
    score DECIMAL(5,2),
    error_message TEXT,
    additional_info TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    feedback TEXT,
    stdout TEXT,
    stderr TEXT,
    compiled BOOLEAN,
    compile_message TEXT,
    execution_time BIGINT,
    memory_usage BIGINT,
    user_answer TEXT,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP,
    CONSTRAINT fk_evaluation_submission FOREIGN KEY (submission_id) REFERENCES experiment_submission(id) ON DELETE CASCADE,
    CONSTRAINT fk_evaluation_task FOREIGN KEY (task_id) REFERENCES experiment_task(id) ON DELETE CASCADE
);

-- 创建资源表
CREATE TABLE resource (
    id VARCHAR(36) PRIMARY KEY,
    experiment_id VARCHAR(36),
    resource_type VARCHAR(20) NOT NULL CHECK (resource_type IN ('DOCUMENT', 'IMAGE', 'VIDEO', 'CODE', 'OTHER', 'SUBMISSION', 'PRESENTATION', 'SPREADSHEET', 'AUDIO', 'ARCHIVE')),
    resource_path VARCHAR(255) NOT NULL,
    file_name VARCHAR(100),
    file_size BIGINT,
    mime_type VARCHAR(50),
    description TEXT,
    upload_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resource_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_resource_experiment_id ON resource(experiment_id);
CREATE INDEX idx_resource_type ON resource(resource_type);
CREATE INDEX idx_resource_upload_time ON resource(upload_time);
CREATE INDEX idx_experiment_creator ON experiment(creator_id);
CREATE INDEX idx_experiment_status ON experiment(status);
CREATE INDEX idx_task_experiment ON experiment_task(experiment_id);
CREATE INDEX idx_assignment_user ON experiment_assignment(user_id);
CREATE INDEX idx_submission_user ON experiment_submission(user_id);
CREATE INDEX idx_submission_task ON experiment_submission(task_id);
CREATE INDEX idx_evaluation_submission ON experiment_evaluation(submission_id);
CREATE INDEX idx_evaluation_user ON experiment_evaluation(user_id);