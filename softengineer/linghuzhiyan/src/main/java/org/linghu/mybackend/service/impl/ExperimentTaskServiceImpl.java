package org.linghu.mybackend.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.linghu.mybackend.constants.TaskType;
import org.linghu.mybackend.domain.ExperimentTask;
import org.linghu.mybackend.dto.ExperimentTaskDTO;
import org.linghu.mybackend.dto.ExperimentTaskRequestDTO;
import org.linghu.mybackend.dto.SourceCodeFileDTO;
import org.linghu.mybackend.repository.ExperimentRepository;
import org.linghu.mybackend.repository.ExperimentTaskRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.linghu.mybackend.service.ExperimentTaskService;
import org.linghu.mybackend.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 实验任务管理服务实现类
 */
@Service
public class ExperimentTaskServiceImpl implements ExperimentTaskService {
    private final ExperimentTaskRepository experimentTaskRepository;
    private final ExperimentRepository experimentRepository;
    private final UserRepository userRepository;

    @Autowired
    public ExperimentTaskServiceImpl(ExperimentTaskRepository experimentTaskRepository,
            ExperimentRepository experimentRepository,
            UserRepository userRepository) {
        this.experimentTaskRepository = experimentTaskRepository;
        this.experimentRepository = experimentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ExperimentTaskDTO createTask(String experimentId, ExperimentTaskRequestDTO requestDTO) {
        // 验证实验是否存在
    if (!experimentRepository.existsById(experimentId)) {
            throw new RuntimeException("实验不存在");
        }

    // 权限检查：仅实验创建者可创建任务
    ensureOwnerOfExperiment(experimentId, "无权为该实验创建任务");

        // 获取当前实验的最大顺序号
        int maxOrder = experimentTaskRepository.findMaxOrderNumByExperimentId(experimentId);
        int nextOrder = maxOrder + 1;

        // 处理问题ID - 将Object类型的question字段转换为JSON字符串
        String questionIdsJson = null;
        if (requestDTO.getQuestion() != null) {
            if (requestDTO.getQuestion() instanceof List ||
                    requestDTO.getQuestion().getClass().isArray()) {
                questionIdsJson = JsonUtils.toJsonString(requestDTO.getQuestion());
            }
        }

        ExperimentTask task = ExperimentTask.builder()
                .id(UUID.randomUUID().toString())
                .experimentId(experimentId)
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .taskType(requestDTO.getTaskType() != null ? requestDTO.getTaskType() : TaskType.OTHER)
                .orderNum(nextOrder)
                .questionIds(questionIdsJson) // 使用处理后的JSON字符串
                .required(requestDTO.getRequired())
                .build();
        ExperimentTask savedTask = experimentTaskRepository.save(task);

        return convertToDTO(savedTask, null);
    }

    @Override
    public List<ExperimentTaskDTO> getTasksByExperimentId(String experimentId) {
        List<ExperimentTask> tasks = experimentTaskRepository.findByExperimentIdOrderByOrderNumAsc(experimentId);

        return tasks.stream()
                .map(task -> convertToDTO(task, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExperimentTaskDTO updateTask(String id, ExperimentTaskRequestDTO requestDTO) {
        ExperimentTask task = experimentTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

    // 权限检查：仅实验创建者可更新任务
    ensureOwnerOfExperiment(task.getExperimentId(), "无权更新该实验任务");

        task.setTitle(requestDTO.getTitle());
        task.setDescription(requestDTO.getDescription());
        task.setTaskType(requestDTO.getTaskType() != null ? requestDTO.getTaskType() : task.getTaskType());
        task.setRequired(requestDTO.getRequired());

        String questionIdsJson = null;
        if (requestDTO.getQuestion() != null) {
            if (requestDTO.getQuestion() instanceof List ||
                    requestDTO.getQuestion().getClass().isArray()) {
                questionIdsJson = JsonUtils.toJsonString(requestDTO.getQuestion());
            }
        }
        task.setQuestionIds(questionIdsJson);

        ExperimentTask updatedTask = experimentTaskRepository.save(task);
        return convertToDTO(updatedTask, null);
    }

    @Override
    @Transactional
    public void deleteTask(String id) {
    if (!experimentTaskRepository.existsById(id)) {
            throw new RuntimeException("任务不存在");
        }
    // 权限检查：仅实验创建者可删除任务
    ExperimentTask task = experimentTaskRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("任务不存在"));

    ensureOwnerOfExperiment(task.getExperimentId(), "无权删除该实验任务");

    experimentTaskRepository.deleteById(id);
    }

    @Override
    @Transactional
    public List<ExperimentTaskDTO> adjustTaskOrder(String experimentId, List<Map<String, String>> taskOrderList) {
        // 校验实验是否存在
        if (!experimentRepository.existsById(experimentId)) {
            throw new RuntimeException("实验不存在");
        }

    // 权限检查：仅实验创建者可调整任务顺序
    ensureOwnerOfExperiment(experimentId, "无权调整该实验任务顺序");

        // 更新每个任务的顺序
        for (Map<String, String> taskOrder : taskOrderList) {
            String taskId = taskOrder.get("id");
            int order = Integer.parseInt(taskOrder.get("order"));

            ExperimentTask task = experimentTaskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));

            // 确保任务属于指定实验
            if (!task.getExperimentId().equals(experimentId)) {
                throw new RuntimeException("任务不属于指定实验");
            }

            task.setOrderNum(order);
            experimentTaskRepository.save(task);
        }

        // 获取更新后的任务列表
        List<ExperimentTask> tasks = experimentTaskRepository.findByExperimentIdOrderByOrderNumAsc(experimentId);
        return tasks.stream()
                .map(task -> convertToDTO(task, null))
                .collect(Collectors.toList());
    }

    /**
     * 校验当前登录用户是否为实验创建者
     */
    protected void ensureOwnerOfExperiment(String experimentId, String messageIfDenied) {

        var experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new RuntimeException("实验不存在"));
        String username = getCurrentUsernameFromSecurityContext();
        if (username == null || "anonymousUser".equals(username)) {
            throw new AccessDeniedException("未认证或权限不足");
        }
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!experiment.getCreatorId().equals(user.getId())) {
            throw new AccessDeniedException(messageIfDenied != null ? messageIfDenied : "权限不足");
        }
    }

    /**
     * 将实验任务实体转换为DTO
     * 
     * @param task            实验任务实体
     * @param sourceCodeFiles 源代码文件列表（可为null）
     * @return 实验任务DTO
     */
    private ExperimentTaskDTO convertToDTO(ExperimentTask task, List<SourceCodeFileDTO> sourceCodeFiles) {
        return ExperimentTaskDTO.builder()
                .id(task.getId())
                .experimentId(task.getExperimentId())
                .title(task.getTitle())
                .description(task.getDescription())
                .taskType(task.getTaskType())
                .orderNum(task.getOrderNum())
                .required(task.getRequired())
                .files(sourceCodeFiles)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
    /**
     * 获取当前认证用户的用户名
     *
     * @return 当前用户名，若未认证则返回null
     */
    protected String getCurrentUsernameFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }
}
