package org.linghu.mybackend.service.impl;

import org.linghu.mybackend.domain.*;
import org.linghu.mybackend.dto.*;
import org.linghu.mybackend.repository.*;
import org.linghu.mybackend.service.QuestionService;
import org.linghu.mybackend.service.StudentExperimentService;
import org.linghu.mybackend.util.MinioUtil;
import org.linghu.mybackend.constants.TaskType;
import org.linghu.mybackend.utils.JsonUtils;
import org.linghu.mybackend.config.JudgeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.minio.messages.Item;
import io.minio.Result;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生实验参与服务实现类
 */
@Service
public class StudentExperimentServiceImpl implements StudentExperimentService {
    private static final Logger logger = LoggerFactory.getLogger(StudentExperimentServiceImpl.class);

    private final ExperimentRepository experimentRepository;
    private final UserRepository userRepository;
    private final ExperimentTaskRepository experimentTaskRepository;
    private final ExperimentAssignmentRepository assignmentRepository;
    private final ExperimentSubmissionRepository submissionRepository;
    private final ExperimentEvaluationRepository evaluationRepository;
    private final QuestionService questionService;
    private final MinioUtil minioUtil;
    private final JudgeConfig judgeConfig;

    @Autowired
    public StudentExperimentServiceImpl(
            ExperimentRepository experimentRepository,
            UserRepository userRepository,
            ExperimentTaskRepository experimentTaskRepository,
            ExperimentAssignmentRepository assignmentRepository,
            ExperimentSubmissionRepository submissionRepository,
            ExperimentEvaluationRepository evaluationRepository,
            QuestionService questionService,
            MinioUtil minioUtil,
            JudgeConfig judgeConfig) {
        this.experimentRepository = experimentRepository;
        this.userRepository = userRepository;
        this.experimentTaskRepository = experimentTaskRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.evaluationRepository = evaluationRepository;
        this.questionService = questionService;
        this.minioUtil = minioUtil;
        this.judgeConfig = judgeConfig;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentDTO> getStudentExperiments(String username) {
        // 获取所有已发布的实验
        List<Experiment> publishedExperiments = experimentRepository
                .findByStatus(Experiment.ExperimentStatus.PUBLISHED);

        return publishedExperiments.stream()
                .map(this::convertToExperimentDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExperimentDTO getExperimentDetails(String expId) {
        // 获取实验详情
        Experiment experiment = findExperimentById(expId);

        return convertToExperimentDTO(experiment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentTaskDTO> getAssignedTasks(String username) {
        User user = findUserByUsername(username);

        // 获取分配给该学生的任务ID列表
        List<String> assignedTaskIds = assignmentRepository.findTaskIdsByUserId(user.getId());

        // 根据任务ID获取任务详情
        List<ExperimentTask> tasks = experimentTaskRepository.findAllById(assignedTaskIds);

        return tasks.stream()
                .map(this::convertToTaskDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional()
    public ExperimentTaskDTO getTaskById(String taskId, String username) {
        ExperimentTask task = findTaskById(taskId);

        // 如果是编程题，从MinIO中获取源代码
        List<SourceCodeFileDTO> sourceCodeFiles = null;
        if (task.getTaskType() != null && task.getTaskType() == TaskType.CODE) {
            sourceCodeFiles = getSourceCodeFromMinio(task.getExperimentId(), taskId);
        }

        // 如果是OTHER类型，需要获取题目信息
        Object questionInfo = null;
        if (task.getTaskType() != null && task.getTaskType() == TaskType.OTHER && task.getQuestionIds() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<String> questionIdsArray = JsonUtils.parseObject(task.getQuestionIds(), List.class);

                if (questionIdsArray != null && !questionIdsArray.isEmpty()) {
                    // 获取题目对象列表
                    List<QuestionDTO> questions = questionService.getQuestionsByIds(questionIdsArray);

                    // 移除答案信息，只保留题目信息
                    for (QuestionDTO question : questions) {
                        question.setAnswer(null); // 不设置答案
                        question.setExplanation(null); // 不设置解析
                    }

                    questionInfo = questions;
                }
            } catch (Exception e) {
                logger.error("解析题目ID异常: " + e.getMessage(), e);
            }
        }

        return convertToTaskDTO(task, sourceCodeFiles, questionInfo);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExperimentEvaluationDTO getTaskEvaluationResult(String taskId, String username) {
        User user = findUserByUsername(username);
        ExperimentTask task = findTaskById(taskId);

        // 查找该任务的最新提交
        Optional<ExperimentSubmission> submission = submissionRepository.findByTaskIdAndUserId(taskId,
                user.getId());

        if (!submission.isPresent()) {
            throw new RuntimeException("未找到该任务的提交记录");
        }

        // CODE类型需要专门的评测服务，这里只处理非CODE类型的自动评测
        if (task.getTaskType() != TaskType.CODE) {
            return autoEvaluateNonCodeTask(task, submission.get(), user.getId());
        }

        // 对于CODE类型，调用代码评测服务
        try {
            return evaluateCodeTask(task, submission.get(), user.getId());
        } catch (Exception e) {
            logger.error("代码评测失败: " + e.getMessage(), e);

            // 返回评测失败结果
            return ExperimentEvaluationDTO.builder()
                    .submissionId(submission.get().getId())
                    .taskId(taskId)
                    .userId(user.getId())
                    .score(BigDecimal.ZERO)
                    .status("ERROR")
                    .errorMessage("代码评测失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 自动评测非编程类型的任务
     *
     * @param task       实验任务
     * @param submission 学生提交
     * @param userId     用户ID
     * @return 评测结果DTO
     */
    private ExperimentEvaluationDTO autoEvaluateNonCodeTask(ExperimentTask task, ExperimentSubmission submission,
                                                            String userId) {
        String userAnswer = submission.getUserAnswer();
        Map<String, Object> expectedAnswers = new HashMap<>();
        Map<String, Object> userAnswers = new HashMap<>();
        BigDecimal score = BigDecimal.ZERO;
        int totalQuestions = 0;
        int correctAnswers = 0;
        StringBuilder feedback = new StringBuilder();
        try {
            // 解析任务中的问题ID列表
            if (task.getQuestionIds() != null && !task.getQuestionIds().isEmpty()) {
                @SuppressWarnings("unchecked")
                List<String> questionIdsArray = JsonUtils.parseObject(task.getQuestionIds(), List.class);

                if (questionIdsArray != null && !questionIdsArray.isEmpty()) {
                    // 获取题目对象列表
                    List<QuestionDTO> questions = questionService.getQuestionsByIds(questionIdsArray);

                    // 构建预期答案映射（题目ID -> 标准答案）
                    for (QuestionDTO question : questions) {
                        expectedAnswers.put(question.getId(), question.getAnswer());
                    }
                }
            }

            // 解析用户提交的答案
            if (userAnswer != null && !userAnswer.isEmpty()) {
                // 尝试将用户答案解析为JSON
                @SuppressWarnings("unchecked")
                Map<String, Object> parsedAnswers = JsonUtils.parseObject(userAnswer, Map.class);
                if (parsedAnswers != null) {
                    userAnswers = parsedAnswers;
                } else {
                    // 如果不是JSON格式，则作为单个答案处理
                    userAnswers = new HashMap<>();
                    userAnswers.put("answer", userAnswer);
                }
            }

            // 比较答案并计算得分
            if (expectedAnswers != null && !expectedAnswers.isEmpty()) {
                totalQuestions = expectedAnswers.size();

                // 遍历预期答案进行比较
                for (Map.Entry<String, Object> entry : expectedAnswers.entrySet()) {
                    String questionId = entry.getKey();
                    Object expectedAnswer = entry.getValue();
                    Object userAns = userAnswers.get(questionId);

                    if (userAns != null) {
                        boolean isCorrect = compareAnswers(expectedAnswer, userAns);
                        if (isCorrect) {
                            correctAnswers++;
                            feedback.append("问题").append(questionId).append(": 正确✓\n");
                        } else {
                            // 格式化答案显示，更友好地显示中文和其他文本
                            String expectedDisplay = formatAnswerForDisplay(expectedAnswer);
                            String actualDisplay = formatAnswerForDisplay(userAns);

                            feedback.append("问题").append(questionId).append(": 错误✗\n")
                                    .append("- 您的答案: ").append(actualDisplay).append("\n")
                                    .append("- 正确答案: ").append(expectedDisplay).append("\n\n");
                        }
                    } else {
                        feedback.append("问题").append(questionId).append(": 未作答\n");
                    }
                }

                // 计算得分 (满分为100)
                if (totalQuestions > 0) {
                    score = new BigDecimal(correctAnswers * 100.0 / totalQuestions).setScale(2,
                            java.math.RoundingMode.HALF_UP);
                }
            }

            // 创建并保存评测结果
            ExperimentEvaluation evaluationEntity = ExperimentEvaluation.builder()
                    .id(UUID.randomUUID().toString())
                    .submissionId(submission.getId())
                    .userId(userId)
                    .taskId(task.getId())
                    .score(score)
                    .additionalInfo("自动评分: 共" + totalQuestions + "题，正确" + correctAnswers + "题")
                    .errorMessage(feedback.toString())
                    .build();
            logger.info("自动评测结果构建完成");
            ExperimentEvaluation savedEvaluation = evaluationRepository.save(evaluationEntity);
            logger.info("自动评测结果已保存: ");
            // 返回评测结果DTO
            return convertToEvaluationDTO(savedEvaluation);
        } catch (Exception e) {
            // 如果自动评测过程中出错，返回错误信息
            logger.error("自动评测失败: " + e.getMessage(), e);

            ExperimentEvaluation evaluationEntity = ExperimentEvaluation.builder()
                    .id(UUID.randomUUID().toString())
                    .submissionId(submission.getId())
                    .userId(userId)
                    .taskId(task.getId())
                    .score(BigDecimal.ZERO)
                    .errorMessage("自动评测失败: " + e.getMessage())
                    .build();

            ExperimentEvaluation savedEvaluation = evaluationRepository.save(evaluationEntity);
            return convertToEvaluationDTO(savedEvaluation);
        }
    }

    /**
     * 比较预期答案和用户答案是否匹配
     *
     * @param expected 预期答案
     * @param actual   用户答案
     * @return 是否匹配
     */
    private boolean compareAnswers(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return true;
        }

        if (expected == null || actual == null) {
            return false;
        } // 处理不同类型的答案比较
        if (expected instanceof String && actual instanceof String) {
            // 对于中文字符串，优先使用中文填空题特殊处理
            String expectedStr = (String) expected;
            String actualStr = (String) actual;

            // 检查是否含有中文字符，如果有则使用特殊的中文答案比较
            if (containsChineseCharacter(expectedStr) || containsChineseCharacter(actualStr)) {
                return compareChineseFillBlankAnswers(expectedStr, actualStr);
            }

            // 非中文答案使用标准比较
            expectedStr = normalizeString(expectedStr);
            actualStr = normalizeString(actualStr);
            return expectedStr.equalsIgnoreCase(actualStr);
        } else if (expected instanceof List && actual instanceof List) {
            // 列表比较 - 检查是否包含相同元素（用于多选题）
            List<?> expectedList = (List<?>) expected;
            List<?> actualList = (List<?>) actual;

            if (expectedList.size() != actualList.size()) {
                return false;
            } // 标准化并转换为小写字符串集合进行比较（支持中文和其他语言）
            Set<String> expectedSet = expectedList.stream()
                    .map(Object::toString)
                    .map(this::normalizeString)
                    .map(String::toLowerCase)
                    .collect(java.util.stream.Collectors.toSet());

            Set<String> actualSet = actualList.stream()
                    .map(Object::toString)
                    .map(this::normalizeString)
                    .map(String::toLowerCase)
                    .collect(java.util.stream.Collectors.toSet());

            return expectedSet.equals(actualSet);
        } else if (expected instanceof Map && actual instanceof Map) {
            // 对象比较 - 递归比较每个字段
            Map<?, ?> expectedMap = (Map<?, ?>) expected;
            Map<?, ?> actualMap = (Map<?, ?>) actual;

            if (expectedMap.size() != actualMap.size()) {
                return false;
            }

            for (Map.Entry<?, ?> entry : expectedMap.entrySet()) {
                Object key = entry.getKey();
                if (!actualMap.containsKey(key) || !compareAnswers(entry.getValue(), actualMap.get(key))) {
                    return false;
                }
            }

            return true;
        }

        // 其他类型直接调用equals比较
        return expected.equals(actual);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentEvaluationDTO> getTaskEvaluationHistory(String taskId, String username) {
        User user = findUserByUsername(username);

        // 查询当前用户在该任务下的所有评测
        List<ExperimentEvaluation> evaluations = evaluationRepository
                .findByUserIdAndTaskIdOrderByIdDesc(user.getId(), taskId);

        // 转换为DTO
        return evaluations.stream()
                .map(this::convertToEvaluationDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExperimentSubmissionDTO submitTask(SubmissionRequestDTO submissionRequest, String username) {
        User user = findUserByUsername(username);
        String taskId = submissionRequest.getTaskId();
        ExperimentTask task = findTaskById(taskId);

        // 校验实验ID是否匹配
        if (!Objects.equals(task.getExperimentId(), submissionRequest.getExperimentId())) {
            throw new RuntimeException("提交的实验ID与任务所属实验ID不匹配");
        }

        // 创建任务提交记录
        String submissionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        String userAnswer = null; // 根据任务类型处理提交内容

        if (task.getTaskType() != null && task.getTaskType() == TaskType.CODE) {
            // 编程题提交处理
            try {
                // 优先处理DTO中的文件列表（直接提交的代码文件）
                if (submissionRequest.getFiles() != null && !submissionRequest.getFiles().isEmpty()) {
                    // 直接使用SubmissionRequestDTO对象
                    // 上传代码文件
                    List<String> uploadedPaths = minioUtil.uploadStudentCodeSubmission(user.getId(), submissionRequest);

                    // 使用JSON格式存储文件路径列表
                    Map<String, Object> answerData = new HashMap<>();
                    answerData.put("paths", uploadedPaths);
                    answerData.put("fileCount", uploadedPaths.size());
                    answerData.put("fileNames", submissionRequest.getFiles().stream()
                            .map(SourceCodeFileDTO::getFileName)
                            .collect(Collectors.toList()));
                    answerData.put("timestamp", System.currentTimeMillis());
                    userAnswer = JsonUtils.toJsonString(answerData);
                }
                // 处理Map类型的答案
                else if (submissionRequest.getUserAnswer() instanceof Map) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        // 尝试获取必要的字段
                        Map<String, Object> answerMap = (Map<String, Object>) submissionRequest.getUserAnswer();

                        // 验证提交内容
                        Object mapTaskId = answerMap.get("taskId");
                        Object mapExpId = answerMap.get("experimentId");
                        Object mapFiles = answerMap.get("files");

                        if (mapTaskId != null && mapExpId != null && mapFiles != null &&
                                !taskId.equals(mapTaskId.toString()) ||
                                !task.getExperimentId().equals(mapExpId.toString())) {
                            throw new RuntimeException("提交的任务ID或实验ID与当前任务不匹配");
                        }

                        // 如果包含files字段，尝试处理源代码文件
                        if (mapFiles instanceof List && !((List) mapFiles).isEmpty()) {
                            List<SourceCodeFileDTO> files = objectMapper.convertValue(
                                    mapFiles, objectMapper.getTypeFactory().constructCollectionType(
                                            List.class, SourceCodeFileDTO.class));

                            // 创建一个临时的SubmissionRequestDTO来上传文件
                            SubmissionRequestDTO tempSubmission = SubmissionRequestDTO.builder()
                                    .experimentId(task.getExperimentId())
                                    .taskId(taskId)
                                    .files(files)
                                    .build();

                            // 上传所有代码文件
                            List<String> uploadedPaths = minioUtil.uploadStudentCodeSubmission(user.getId(),
                                    tempSubmission);

                            // 使用JSON格式存储文件路径列表
                            Map<String, Object> answerData = new HashMap<>();
                            answerData.put("paths", uploadedPaths);
                            answerData.put("fileCount", uploadedPaths.size());
                            answerData.put("fileNames", files.stream()
                                    .map(SourceCodeFileDTO::getFileName)
                                    .collect(Collectors.toList()));
                            answerData.put("timestamp", System.currentTimeMillis());
                            userAnswer = JsonUtils.toJsonString(answerData);
                        } else {
                            // 如果没有files字段，则作为普通答案处理
                            userAnswer = JsonUtils.toJsonString(submissionRequest.getUserAnswer());
                        }
                    } catch (Exception e) {
                        // 处理失败则作为普通答案处理
                        userAnswer = JsonUtils.toJsonString(submissionRequest.getUserAnswer());
                    }
                } // 如果是字符串形式的代码，将其保存为文件
                else if (submissionRequest.getUserAnswer() != null) {
                    String content = submissionRequest.getUserAnswer().toString();
                    SourceCodeFileDTO codeFile = SourceCodeFileDTO.builder()
                            .fileName("submission.txt")
                            .content(content)
                            .build();

                    // 创建临时提交请求对象用于上传
                    SubmissionRequestDTO tempSubmission = SubmissionRequestDTO.builder()
                            .experimentId(task.getExperimentId())
                            .taskId(taskId)
                            .files(List.of(codeFile))
                            .build();

                    // 上传文件并存储路径
                    List<String> uploadedPaths = minioUtil.uploadStudentCodeSubmission(user.getId(), tempSubmission);

                    // 使用JSON格式存储文件路径
                    Map<String, Object> answerData = new HashMap<>();
                    answerData.put("paths", uploadedPaths);
                    answerData.put("fileCount", uploadedPaths.size());
                    answerData.put("fileName", "submission.txt");
                    answerData.put("timestamp", System.currentTimeMillis());
                    userAnswer = JsonUtils.toJsonString(answerData);
                }
            } catch (Exception e) {
                throw new RuntimeException("处理代码提交失败: " + e.getMessage(), e);
            }
        } else {
            // 非代码题提交处理
            Object submission = submissionRequest.getUserAnswer();
            if (submission instanceof Map) {
                try {
                    userAnswer = JsonUtils.toJsonString(submission);
                } catch (Exception e) {
                    throw new RuntimeException("处理答案内容失败: " + e.getMessage(), e);
                }
            } else if (submission != null) {
                userAnswer = submission.toString();
            }
        }

        // 转换为数据库实体
        ExperimentSubmission submissionEntity = ExperimentSubmission.builder()
                .id(submissionId)
                .taskId(taskId)
                .userId(user.getId())
                .userAnswer(userAnswer)
                .submitTime(LocalDateTime.now())
                .build();

        // 保存提交记录
        ExperimentSubmission savedSubmission = submissionRepository.save(submissionEntity);

        // 将用户答案转换为适当格式
        Object formattedUserAnswer;
        if (task.getTaskType() == TaskType.CODE && userAnswer != null && !userAnswer.isEmpty()) {
            // 对于CODE类型，解析JSON格式的答案
            formattedUserAnswer = JsonUtils.parseObject(userAnswer, Object.class);
            if (formattedUserAnswer == null) {
                // 如果解析失败则返回原始字符串
                formattedUserAnswer = userAnswer;
            }
        } else {
            formattedUserAnswer = userAnswer;
        }

        // 构建并返回DTO
        return ExperimentSubmissionDTO.builder().id(savedSubmission.getId()).task_id(taskId).user_id(user.getId())
                .submitTime(now).user_answer(formattedUserAnswer).build();
    }

    // 辅助方法

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    private Experiment findExperimentById(String id) {
        return experimentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("实验不存在"));
    }

    private ExperimentTask findTaskById(String id) {
        return experimentTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("实验任务不存在"));
    }

    private ExperimentDTO convertToExperimentDTO(Experiment experiment) {
        ExperimentDTO dto = new ExperimentDTO();
        dto.setId(experiment.getId());
        dto.setName(experiment.getName());
        dto.setDescription(experiment.getDescription());
        dto.setCreator_Id(experiment.getCreatorId());
        dto.setStatus(experiment.getStatus());
        dto.setStartTime(experiment.getStartTime());
        dto.setEndTime(experiment.getEndTime());
        return dto;
    }

    private ExperimentTaskDTO convertToTaskDTO(ExperimentTask task) {
        return convertToTaskDTO(task, null);
    }

    private ExperimentTaskDTO convertToTaskDTO(ExperimentTask task, List<SourceCodeFileDTO> sourceCodeFiles) {
        return convertToTaskDTO(task, sourceCodeFiles, null);
    }

    private ExperimentTaskDTO convertToTaskDTO(ExperimentTask task, List<SourceCodeFileDTO> sourceCodeFiles,
                                               Object questionInfo) {
        ExperimentTaskDTO dto = new ExperimentTaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setExperimentId(task.getExperimentId());
        dto.setTaskType(task.getTaskType());
        dto.setOrderNum(task.getOrderNum());
        dto.setRequired(task.getRequired());
        dto.setFiles(sourceCodeFiles); // 现在支持源代码文件

        // 如果是OTHER类型且有题目信息，设置question字段
        if (task.getTaskType() == TaskType.OTHER && questionInfo != null) {
            dto.setQuestion(questionInfo);
        }

        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        return dto;
    }

    private ExperimentEvaluationDTO convertToEvaluationDTO(ExperimentEvaluation evaluation) {
        ExperimentSubmission submission = null;
        String taskId = null;
        String userId = null;
        LocalDateTime submitTime = null;
        String userAnswer = null;
        String status = "EVALUATED";

        if (evaluation.getSubmissionId() != null) {
            Optional<ExperimentSubmission> optSubmission = submissionRepository.findById(evaluation.getSubmissionId());
            if (optSubmission.isPresent()) {
                submission = optSubmission.get();
                taskId = submission.getTaskId();
                userId = submission.getUserId();
                submitTime = submission.getSubmitTime();
                userAnswer = submission.getUserAnswer();
            }
        }

        return ExperimentEvaluationDTO.builder()
                .id(evaluation.getId())
                .submissionId(evaluation.getSubmissionId())
                .score(evaluation.getScore())
                .errorMessage(evaluation.getErrorMessage())
                .additionalInfo(evaluation.getAdditionalInfo())
                .taskId(taskId)
                .userId(userId)
                .submitTime(submitTime)
                .userAnswer(userAnswer).status(status)
                .build();
    }

    /**
     * 从MinIO获取实验源代码
     *
     * @param experimentId 实验ID
     * @param taskId       任务ID
     * @return 源代码文件列表，如果没有源代码文件则返回null
     */
    private List<SourceCodeFileDTO> getSourceCodeFromMinio(String experimentId, String taskId) {
        try {
            // 尝试不同的路径模式来获取源代码文件
            List<String> possiblePrefixes = Arrays.asList(
                    experimentId + "/experiment/" + taskId + "/" // 不包含 original 目录的路径
            );

            List<SourceCodeFileDTO> sourceCodeFiles = new ArrayList<>();
            boolean filesFound = false;

            for (String prefix : possiblePrefixes) {
                try {
                    Iterable<Result<Item>> codeItems = minioUtil.listObjects("resource", prefix);
                    Iterator<Result<Item>> iterator = codeItems.iterator();

                    // 检查是否有文件
                    if (iterator.hasNext()) {
                        filesFound = true;

                        // 遍历所有文件
                        for (Result<Item> result : codeItems) {
                            Item item = result.get();
                            String objectName = item.objectName();

                            // 跳过目录
                            if (objectName.endsWith("/") || objectName.contains("/original/")) {
                                continue;
                            }
                            // 下载并读取文件内容
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(minioUtil.downloadFile(objectName).getInputStream()))) {

                                // 获取文件名
                                String fileName = objectName.startsWith(prefix) ? objectName.substring(prefix.length())
                                        : objectName.substring(objectName.lastIndexOf("/") + 1);

                                // 读取文件内容
                                StringBuilder contentBuilder = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    contentBuilder.append(line).append("\n");
                                }

                                // 创建源代码文件DTO
                                sourceCodeFiles.add(SourceCodeFileDTO.builder()
                                        .fileName(fileName)
                                        .content(contentBuilder.toString())
                                        .build());
                            }
                        }

                        // 找到文件后退出循环
                        break;
                    }
                } catch (Exception e) {
                    // 记录当前路径尝试的错误但继续尝试其他路径
                    System.err.println("尝试从前缀 " + prefix + " 获取文件失败: " + e.getMessage());
                }
            }

            return filesFound ? sourceCodeFiles : null;

        } catch (Exception e) {
            // 记录错误但不抛出异常，允许任务正常返回（只是没有源代码）
            System.err.println("获取实验源代码失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * /**
     * 从MinIO获取学生代码文件内容
     *
     * @param filePath MinIO中的文件路径
     * @return 文件内容字符串
     */
    private String getFileContentFromMinio(String filePath) {
        try {
            org.springframework.core.io.Resource fileResource = minioUtil.downloadFile(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileResource.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            return content.toString();
        } catch (Exception e) {
            System.err.println("从MinIO获取文件内容失败: " + e.getMessage());
            return "[无法获取文件内容]";
        }
    }

    /**
     * 标准化字符串，用于比较时处理中文和其他特殊字符
     *
     * @param input 输入字符串
     * @return 标准化后的字符串
     */
    private String normalizeString(String input) {
        if (input == null) {
            return "";
        }

        // 去除首尾空格
        String result = input.trim();

        // 替换多个空格为单个空格
        result = result.replaceAll("\\s+", " ");

        // 移除标点符号（包括中文标点）
        result = result.replaceAll("[\\p{Punct}\\p{IsPunctuation}]", "");

        // 标准化中文全角字符到半角字符
        result = normalizeFullWidthChars(result);

        return result;
    }

    /**
     * 将全角字符转换为半角字符
     *
     * @param input 输入字符串
     * @return 转换后的字符串
     */
    private String normalizeFullWidthChars(String input) {
        if (input == null) {
            return "";
        }

        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] >= 0xFF01 && chars[i] <= 0xFF5E) {
                // 全角字符范围
                chars[i] = (char) (chars[i] - 0xFEE0);
            } else if (chars[i] == 0x3000) {
                // 全角空格
                chars[i] = ' ';
            }
        }

        return new String(chars);
    }

    /**
     * 格式化答案用于显示，使其对用户更友好
     *
     * @param answer 原始答案对象
     * @return 格式化后的显示字符串
     */
    private String formatAnswerForDisplay(Object answer) {
        if (answer == null) {
            return "未作答";
        }

        if (answer instanceof String) {
            return (String) answer;
        } else if (answer instanceof List) {
            List<?> list = (List<?>) answer;
            if (list.isEmpty()) {
                return "[]";
            }

            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ", "[", "]"));
        } else if (answer instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) answer;
            if (map.isEmpty()) {
                return "{}";
            }

            return map.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining(", ", "{", "}"));
        }

        return answer.toString();
    }

    /**
     * 特殊处理填空题答案比较，支持中文
     *
     * @param expected 预期答案
     * @param actual   实际答案
     * @return 是否匹配
     */
    private boolean compareChineseFillBlankAnswers(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }

        // 标准化处理
        String normalizedExpected = normalizeString(expected);
        String normalizedActual = normalizeString(actual);

        // 严格对比格式化后的字符串，不完全一致就是错误
        return normalizedExpected.equals(normalizedActual);
    }

    /**
     * 检查字符串是否包含中文字符
     *
     * @param str 要检查的字符串
     * @return 是否包含中文字符
     */
    private boolean containsChineseCharacter(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (char c : str.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION) {
                return true;
            }
        }

        return false;
    }

    /**
     * 对CODE类型的任务进行评测
     *
     * @param task       实验任务
     * @param submission 学生提交
     * @param userId     用户ID
     * @return 评测结果DTO
     * @throws Exception 如果评测过程发生错误
     */
    private ExperimentEvaluationDTO evaluateCodeTask(ExperimentTask task, ExperimentSubmission submission,
                                                     String userId) throws Exception {
        // 获取用户提交的代码文件路径
        String userAnswer = submission.getUserAnswer();

        logger.info("开始评测用户 {} 在任务 {} 的代码提交", userId, task.getId());

        // 解析用户提交的文件路径
        @SuppressWarnings("unchecked")
        Map<String, Object> answerData = JsonUtils.parseObject(userAnswer, Map.class);
        if (answerData == null) {
            throw new RuntimeException("无法解析提交的代码信息");
        }

        @SuppressWarnings("unchecked")
        List<String> filePaths = (List<String>) answerData.get("paths");
        if (filePaths == null || filePaths.isEmpty()) {
            throw new RuntimeException("未找到提交的代码文件");
        }

        logger.info("用户 {} 提交了 {} 个代码文件", userId, filePaths.size());

        // 从MinIO获取提交的代码文件内容
        List<Map<String, String>> files = new ArrayList<>();
        for (String path : filePaths) {
            String content = getFileContentFromMinio(path);

            // 提取相对路径，保留目录结构
            String relativePath = path;
            // 典型路径格式: userId/experimentId/taskId/timestamp/根目录/{文件名}
            String[] pathParts = path.split("/");

            // 从该目录之后开始提取路径
            relativePath = String.join("/", Arrays.copyOfRange(pathParts, 5, pathParts.length));


            Map<String, String> fileMap = new HashMap<>();
            fileMap.put("name", relativePath);
            fileMap.put("content", content);
            files.add(fileMap);

            logger.debug("获取到文件 {}, 内容长度: {} 字符", relativePath, content.length());
        }

        logger.info("准备使用提交目录中的compile.sh和run.sh脚本执行评测");

        // 构建评测请求数据 - 使用go judge格式
        Map<String, Object> requestData = new HashMap<>();

        Map<String, Object> cmdMap = new HashMap<>();

        cmdMap.put("args", Arrays.asList("/bin/sh", "-c", "./compile.sh && ./run.sh"));
        cmdMap.put("env", Arrays.asList("PATH=/usr/bin:/bin"));

        // 设置文件流
        List<Map<String, Object>> filesList = new ArrayList<>();
        filesList.add(new HashMap<String, Object>() {
            {
                put("content", "");
            }
        });
        filesList.add(new HashMap<String, Object>() {
            {
                put("name", "stdout");
                put("max", 10240);
            }
        });
        filesList.add(new HashMap<String, Object>() {
            {
                put("name", "stderr");
                put("max", 10240);
            }
        });
        cmdMap.put("files", filesList);

        // 设置资源限制
        cmdMap.put("cpuLimit", 6000000000L);
        cmdMap.put("memoryLimit", 536870912L);
        cmdMap.put("procLimit", 50);

        // 构建copyIn用于上传文件
        Map<String, Object> copyIn = new HashMap<>();

        // 添加用户提交的文件
        Map<String, Set<String>> directories = new HashMap<>();
        for (Map<String, String> file : files) {
            String filePath = file.get("name");

            // 如果文件在子目录中，需要确保目录已经创建
            if (filePath.contains("/")) {
                String dirPath = filePath.substring(0, filePath.lastIndexOf('/'));
                createDirectoryStructure(copyIn, dirPath, directories);
            }

            // 添加文件
            copyIn.put(filePath, new HashMap<String, String>() {
                {
                    put("content", file.get("content"));
                }
            });
        }

        cmdMap.put("copyIn", copyIn);
        cmdMap.put("copyOut", Arrays.asList("stdout", "stderr"));

        // 将cmd添加到requestData
        requestData.put("cmd", Arrays.asList(cmdMap)); // 发送HTTP请求到评测服务
        String evaluationServiceUrl = judgeConfig.getRunUrl();
        String responseJson = sendHttpRequest(evaluationServiceUrl, JsonUtils.toJsonString(requestData));

        // 解析评测结果
        String stdout = "";
        String stderr = "";

        try {
            logger.info("开始解析评测结果: {}", responseJson);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultArray = JsonUtils.parseObject(responseJson, List.class);

            if (resultArray != null && !resultArray.isEmpty()) {
                // 数组格式处理
                logger.info("检测到数组格式的评测结果");
                Map<String, Object> result = resultArray.get(0);

                if (result.containsKey("files")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultFiles = (Map<String, Object>) result.get("files");

                    if (resultFiles != null) {
                        // 直接获取值
                        Object stdoutObj = resultFiles.get("stdout");
                        stdout = stdoutObj != null ? stdoutObj.toString() : "";

                        Object stderrObj = resultFiles.get("stderr");
                        stderr = stderrObj != null ? stderrObj.toString() : "";

                        logger.info("从数组格式中解析到 stdout: {}, stderr: {}", stdout, stderr);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析评测结果出错: " + e.getMessage(), e);
            throw new RuntimeException("解析评测结果出错: " + e.getMessage());
        }

        // 保存评测结果
        ExperimentEvaluation evaluationEntity = ExperimentEvaluation.builder()
                .id(UUID.randomUUID().toString())
                .submissionId(submission.getId())
                .userId(userId)
                .taskId(task.getId())
                .score(stderr.isEmpty() ? new BigDecimal("100") : BigDecimal.ZERO) // 如有错误，得分为0
                .additionalInfo(stdout)
                .errorMessage(stderr)
                .build();

        ExperimentEvaluation savedEvaluation = evaluationRepository.save(evaluationEntity);
        // 如果存在编译或运行错误，立即返回错误结果
        ExperimentEvaluationDTO resultDTO = convertToEvaluationDTO(savedEvaluation);
        if (!stderr.isEmpty()) {
            // 创建新的DTO，设置FAILED状态
            return ExperimentEvaluationDTO.builder()
                    .id(resultDTO.getId())
                    .submissionId(resultDTO.getSubmissionId())
                    .score(resultDTO.getScore())
                    .errorMessage(resultDTO.getErrorMessage())
                    .additionalInfo(resultDTO.getAdditionalInfo())
                    .taskId(resultDTO.getTaskId())
                    .userId(resultDTO.getUserId())
                    .submitTime(resultDTO.getSubmitTime())
                    .userAnswer(resultDTO.getUserAnswer())
                    .status("FAILED")
                    .build();
        }

        // 无错误，返回成功结果
        return ExperimentEvaluationDTO.builder()
                .id(resultDTO.getId())
                .submissionId(resultDTO.getSubmissionId())
                .score(resultDTO.getScore())
                .errorMessage(resultDTO.getErrorMessage())
                .additionalInfo(resultDTO.getAdditionalInfo())
                .taskId(resultDTO.getTaskId())
                .userId(resultDTO.getUserId())
                .submitTime(resultDTO.getSubmitTime())
                .userAnswer(resultDTO.getUserAnswer())
                .status("SUCCESS")
                .build();
    }

    /**
     * 发送HTTP请求到评测服务
     *
     * @param url  请求URL
     * @param json 请求体JSON
     * @return 响应JSON
     * @throws Exception 如果请求失败
     */
    private String sendHttpRequest(String url, String json) throws Exception {
        logger.info("发送评测请求到 {}", url);
        logger.debug("评测请求体: {}", json);

        java.net.URI uri = java.net.URI.create(url);
        java.net.URL apiUrl = uri.toURL();
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) apiUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        // 发送请求体
        try (java.io.OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes("UTF-8");
            os.write(input, 0, input.length);
        }

        // 获取响应
        int responseCode = conn.getResponseCode();
        logger.info("评测服务返回状态码: {}", responseCode);

        if (responseCode != 200) {
            logger.error("评测服务返回错误状态码: {}", responseCode);
            throw new RuntimeException("评测服务返回错误状态码: " + responseCode);
        }

        // 读取响应内容
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            String responseStr = response.toString();
            logger.debug("评测服务响应: {}", responseStr);
            return responseStr;
        }
    }

    /**
     * 为代码评测创建目录结构
     *
     * @param copyIn      评测服务的copyIn映射
     * @param dirPath     需要创建的目录路径
     * @param directories 已创建目录的缓存
     */
    private void createDirectoryStructure(Map<String, Object> copyIn, String dirPath,
                                          Map<String, Set<String>> directories) {
        // 如果目录已经创建过，直接返回
        if (directories.containsKey(dirPath)) {
            return;
        }

        // 分解路径，确保上级目录先被创建
        String[] parts = dirPath.split("/");
        StringBuilder currentPath = new StringBuilder();

        // 逐级创建目录
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                currentPath.append("/");
            }
            currentPath.append(parts[i]);

            String path = currentPath.toString();

            // 记录创建的目录
            if (!directories.containsKey(path)) {
                Set<String> subdirs = new HashSet<>();
                directories.put(path, subdirs);

                // 将当前目录添加到其父目录的子目录集合中
                if (i > 0) {
                    String parentPath = path.substring(0, path.lastIndexOf('/'));
                    if (directories.containsKey(parentPath)) {
                        directories.get(parentPath).add(parts[i]);
                    }
                }

                // 在copyIn中创建目录项
                // 在GoJudge中，目录作为特殊的文件处理，内容为空但路径以/结尾
                copyIn.put(path + "/", new HashMap<String, String>() {
                    {
                        put("content", "");
                    }
                });
            }
        }
    }
}
