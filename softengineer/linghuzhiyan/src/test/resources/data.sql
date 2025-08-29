-- 插入测试题目数据
INSERT INTO question (id, question_type, content, score, options, answer, explanation, tags, created_at, updated_at) VALUES 
('question1', 'SINGLE_CHOICE', 'What is the capital of France?', 5.0, '{"A": "London", "B": "Paris", "C": "Berlin", "D": "Madrid"}', '{"correct": "B"}', 'Paris is the capital city of France.', 'geography,capital,france', NOW(), NOW()),
('q2', 'MULTIPLE_CHOICE', 'Which of the following are programming languages?', 10.0, '{"A": "Java", "B": "Python", "C": "HTML", "D": "C++"}', '{"correct": ["A", "B", "D"]}', 'Java, Python, and C++ are programming languages. HTML is a markup language.', 'programming,languages,technology', NOW(), NOW()),
('q3', 'FILL_BLANK', 'The _____ is the largest planet in our solar system.', 3.0, NULL, '{"correct": "Jupiter"}', 'Jupiter is indeed the largest planet in our solar system.', 'astronomy,solar system,planets', NOW(), NOW()),
('q4', 'QA', 'Explain the concept of object-oriented programming.', 15.0, NULL, '{"keywords": ["encapsulation", "inheritance", "polymorphism", "abstraction"]}', 'OOP is a programming paradigm based on objects and classes.', 'programming,oop,concepts', NOW(), NOW()),
('q5', 'SINGLE_CHOICE', 'What is 2 + 2?', 2.0, '{"A": "3", "B": "4", "C": "5", "D": "6"}', '{"correct": "B"}', '2 + 2 equals 4.', 'math,arithmetic', NOW(), NOW());

-- 插入测试实验数据
INSERT INTO experiment (id, name, description, creator_id, status, start_time, end_time, created_at, updated_at) VALUES 
('experiment1', 'Test Experiment 1', 'Description for test experiment 1', 'creator1', 'PUBLISHED', DATEADD('DAY', -1, NOW()), DATEADD('DAY', 1, NOW()), DATEADD('DAY', -2, NOW()), DATEADD('DAY', -1, NOW())),
('experiment2', 'Test Experiment 2', 'Description for test experiment 2', 'creator1', 'DRAFT', DATEADD('DAY', 1, NOW()), DATEADD('DAY', 3, NOW()), DATEADD('DAY', -1, NOW()), NOW()),
('experiment3', 'Another Experiment', 'Description for another experiment', 'creator2', 'PUBLISHED', DATEADD('DAY', -2, NOW()), DATEADD('DAY', -1, NOW()), DATEADD('DAY', -3, NOW()), DATEADD('DAY', -2, NOW()));

-- 插入测试任务数据
INSERT INTO experiment_task (id, experiment_id, title, description, question_ids, required, order_num, task_type, created_at, updated_at) VALUES 
('task1', 'experiment1', 'Task 1', 'Description for task 1', '["q1", "q2"]', TRUE, 1, 'CODE', DATEADD('DAY', -1, NOW()), NOW()),
('task2', 'experiment1', 'Task 2', 'Description for task 2', '["q3", "q4"]', FALSE, 2, 'OTHER', DATEADD('DAY', -1, NOW()), NOW()),
('task3', 'experiment2', 'Task 3', 'Description for task 3', '["q5"]', TRUE, 1, 'OTHER', DATEADD('DAY', -1, NOW()), NOW()),
('task4', 'experiment1', 'Task 4', 'Description for task 4', '[]', TRUE, 3, 'CODE', DATEADD('DAY', -1, NOW()), NOW());

-- 插入测试分配数据
INSERT INTO experiment_assignment (id, task_id, user_id, assigned_at) VALUES 
('assign1', 'task1', 'user1', NOW()),
('assign2', 'task1', 'user2', NOW()),
('assign3', 'task2', 'user1', NOW());

-- 插入测试提交数据
INSERT INTO experiment_submission (id, task_id, user_id, submission_content, score, grader_id, graded_time, time_spent, submit_time, created_time, updated_time) VALUES 
('sub1', 'task1', 'user1', '{"answer": "solution1"}', 85.50, 'grader1', DATEADD('HOUR', -1, NOW()), 3600, DATEADD('HOUR', -2, NOW()), DATEADD('HOUR', -2, NOW()), DATEADD('HOUR', -1, NOW())),
('sub2', 'task1', 'user2', '{"answer": "solution2"}', 92.00, 'grader1', DATEADD('MINUTE', -30, NOW()), 2400, DATEADD('HOUR', -1, NOW()), DATEADD('HOUR', -1, NOW()), DATEADD('MINUTE', -30, NOW())),
('sub3', 'task2', 'user1', '{"answer": "solution3"}', NULL, NULL, NULL, 1800, DATEADD('MINUTE', -15, NOW()), DATEADD('MINUTE', -15, NOW()), DATEADD('MINUTE', -15, NOW())),
('sub4', 'task1', 'user1', '{"answer": "latest_solution"}', 95.00, 'grader2', DATEADD('MINUTE', -10, NOW()), 4200, DATEADD('MINUTE', -20, NOW()), DATEADD('MINUTE', -20, NOW()), DATEADD('MINUTE', -10, NOW()));

-- 插入测试评测数据
INSERT INTO experiment_evaluation (id, submission_id, user_id, task_id, score, feedback, created_time, updated_time) VALUES 
('eval1', 'sub1', 'user1', 'task1', 85.50, 'Test evaluation feedback 1', DATEADD('HOUR', -2, NOW()), DATEADD('HOUR', -1, NOW())),
('eval2', 'sub1', 'user1', 'task1', NULL, NULL, DATEADD('HOUR', -1, NOW()), NOW()),
('eval3', 'sub2', 'user2', 'task2', 0.00, 'Test failed', DATEADD('MINUTE', -30, NOW()), DATEADD('MINUTE', -15, NOW()));
