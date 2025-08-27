package org.linghu.mybackend.service.impl;

import org.linghu.mybackend.domain.Experiment;
import org.linghu.mybackend.domain.ExperimentAssignment;
import org.linghu.mybackend.domain.User;
import org.linghu.mybackend.dto.UserDTO;
import org.linghu.mybackend.repository.ExperimentAssignmentRepository;
import org.linghu.mybackend.repository.ExperimentRepository;
import org.linghu.mybackend.repository.ExperimentTaskRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.linghu.mybackend.service.ExperimentAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 实验任务分配管理服务实现类
 */
@Service
public class ExperimentAssignmentServiceImpl implements ExperimentAssignmentService {

    private final ExperimentAssignmentRepository assignmentRepository;
    private final ExperimentRepository experimentRepository;
    private final UserRepository userRepository;
    private final ExperimentTaskRepository experimentTaskRepository;

    @Autowired
    public ExperimentAssignmentServiceImpl(
            ExperimentAssignmentRepository assignmentRepository,
            ExperimentRepository experimentRepository,
            UserRepository userRepository, ExperimentTaskRepository experimentTaskRepository) {
        this.assignmentRepository = assignmentRepository;
        this.experimentRepository = experimentRepository;
        this.userRepository = userRepository;
        this.experimentTaskRepository = experimentTaskRepository;
    }

    @Override
    @Transactional
    public void assignTask(String taskId, String userId) { // 验证实验任务和用户是否存在
        experimentTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("实验任务不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查是否已分配
        if (assignmentRepository.existsByTaskIdAndUserId(taskId, userId)) {
            throw new RuntimeException("该实验任务已分配给此用户");
        }

        // 检查用户是否为学生（使用辅助方法）
        if (!isStudentUser(user)) {
            throw new RuntimeException("只能将实验任务分配给学生角色的用户");
        }

        // 创建分配记录
        ExperimentAssignment assignment = new ExperimentAssignment();
        assignment.setId(UUID.randomUUID().toString());
        assignment.setTaskId(taskId);
        assignment.setUserId(userId);

        assignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public void batchAssignTask(String taskId, List<String> userIds) {
        // 验证实验任务是否存在
        experimentTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("实验任务不存在"));

        // 逐个分配
        for (String userId : userIds) {
            try {
                if (!assignmentRepository.existsByTaskIdAndUserId(taskId, userId)) {
                    User user = userRepository.findById(userId).orElse(null);

                    if (user != null && isStudentUser(user)) {

                        ExperimentAssignment assignment = new ExperimentAssignment();
                        assignment.setId(UUID.randomUUID().toString());
                        assignment.setTaskId(taskId);
                        assignment.setUserId(userId);

                        assignmentRepository.save(assignment);
                    }
                }
            } catch (Exception e) {
                // 记录错误但继续处理其他用户
                System.err.println("分配实验任务失败，用户ID: " + userId + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 判断用户是否为学生角色
     *
     * @param user 用户对象
     * @return 是否为学生
     */
    private boolean isStudentUser(User user) {
        // 根据实际情况实现
        // 这里简化处理，实际应该检查用户角色
        return true;
    }

    @Override
    public List<UserDTO> getTaskAssignments(String taskId) {
        // 验证实验任务是否存在
        experimentTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("实验任务不存在"));

        // 获取分配给该实验任务的所有用户ID
        List<String> userIds = assignmentRepository.findByTaskId(taskId)
                .stream()
                .map(ExperimentAssignment::getUserId)
                .collect(Collectors.toList());

        // 获取用户详情
        List<User> users = userRepository.findAllById(userIds);

        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignTaskToAllStudents(String taskId) {
        // 验证实验任务是否存在
        experimentRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("实验任务不存在"));

        // 获取所有学生用户
        List<User> students = getAllStudentUsers();

        // 批量分配
        List<String> studentIds = students.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        batchAssignTask(taskId, studentIds);
    }

    /**
     * 获取所有学生用户
     *
     * @return 学生用户列表
     */
    private List<User> getAllStudentUsers() {
        // 这里需要根据实际情况实现
        // 例如可以通过角色查询所有学生
        return userRepository.findAll();
    }    @Override
    @Transactional
    public void removeTaskAssignment(String taskId, String userId) {
        // 验证分配是否存在
        ExperimentAssignment assignment = assignmentRepository
                .findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new RuntimeException("未找到该实验任务分配"));

        // 删除分配记录
        assignmentRepository.delete(assignment);
    }

    @Override
    @Transactional
    public void batchRemoveTaskAssignment(String taskId, List<String> userIds) {
        // 验证实验任务是否存在
        experimentTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("实验任务不存在"));

        // 逐个取消分配
        for (String userId : userIds) {
            try {
                ExperimentAssignment assignment = assignmentRepository
                        .findByTaskIdAndUserId(taskId, userId)
                        .orElse(null);
                
                if (assignment != null) {
                    assignmentRepository.delete(assignment);
                }
            } catch (Exception e) {
                // 记录错误但继续处理其他用户
                System.err.println("取消实验任务分配失败，用户ID: " + userId + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 将用户实体转换为DTO
     * 
     * @param user 用户实体
     * @return 用户DTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());

        return dto;
    }
}
