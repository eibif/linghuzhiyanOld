-- 创建题目表
CREATE TABLE IF NOT EXISTS question (
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

-- 创建实验表
CREATE TABLE IF NOT EXISTS experiment (
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
CREATE TABLE IF NOT EXISTS experiment_task (
    id VARCHAR(36) PRIMARY KEY,
    experiment_id VARCHAR(36) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    question_ids TEXT,
    required BOOLEAN DEFAULT TRUE,
    order_num INT DEFAULT 0,
    task_type VARCHAR(20) DEFAULT 'OTHER',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 创建实验任务分配表
CREATE TABLE IF NOT EXISTS experiment_assignment (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    UNIQUE (task_id, user_id)
);

-- 创建实验提交表
CREATE TABLE IF NOT EXISTS experiment_submission (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    submission_content TEXT,
    score DECIMAL(5,2),
    grader_id VARCHAR(36),
    graded_time TIMESTAMP,
    time_spent INT,
    submit_time TIMESTAMP NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP
);

-- 创建实验评测表
CREATE TABLE IF NOT EXISTS experiment_evaluation (
    id VARCHAR(36) PRIMARY KEY,
    submission_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    task_id VARCHAR(36) NOT NULL,
    score DECIMAL(5,2),
    feedback TEXT,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP
);
