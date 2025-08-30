package org.linghu.mybackend.service.impl;

import java.util.UUID;

import org.linghu.mybackend.domain.Experiment;
import org.linghu.mybackend.domain.User;
import org.linghu.mybackend.dto.ExperimentDTO;
import org.linghu.mybackend.dto.ExperimentRequestDTO;
import org.linghu.mybackend.repository.ExperimentAssignmentRepository;
import org.linghu.mybackend.repository.ExperimentRepository;
import org.linghu.mybackend.repository.ExperimentTaskRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.linghu.mybackend.service.ExperimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 实验管理服务实现类
 */
@Service
public class ExperimentServiceImpl implements ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final UserRepository userRepository;
    private final ExperimentTaskRepository experimentTaskRepository;
    private final ExperimentAssignmentRepository experimentAssignmentRepository;

    @Autowired
    public ExperimentServiceImpl(ExperimentRepository experimentRepository,
                                 UserRepository userRepository,
                                 ExperimentTaskRepository experimentTaskRepository,
                                 ExperimentAssignmentRepository experimentAssignmentRepository) {
        this.experimentRepository = experimentRepository;
        this.userRepository = userRepository;
        this.experimentTaskRepository = experimentTaskRepository;
        this.experimentAssignmentRepository = experimentAssignmentRepository;
    }

    @Override
    @Transactional
    public ExperimentDTO createExperiment(ExperimentRequestDTO requestDTO, String creatorUsername) {
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Experiment experiment = Experiment.builder()
                .id(UUID.randomUUID().toString())
                .name(requestDTO.getName())
                .description(requestDTO.getDescription())
                .status(requestDTO.getStatus() != null ? requestDTO.getStatus() : Experiment.ExperimentStatus.DRAFT)
                .startTime(requestDTO.getStartTime())
                .endTime(requestDTO.getEndTime())
                .creatorId(creator.getId())
                .build();

        Experiment savedExperiment = experimentRepository.save(experiment);

        return convertToDTO(savedExperiment);
    }

    @Override
    public Page<ExperimentDTO> getAllExperiments(int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<Experiment> experiments = experimentRepository.findAll(pageable);

        return experiments.map(experiment -> {
            return convertToDTO(experiment);
        });
    }

    @Override
    public ExperimentDTO getExperimentById(String id) {
        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("实验不存在"));

        return convertToDTO(experiment);
    }

    @Override
    @Transactional
    public ExperimentDTO updateExperiment(String id, ExperimentRequestDTO requestDTO, String username) {
        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("实验不存在"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        // 检查权限（仅创建者可以更新）
        if (!experiment.getCreatorId().equals(user.getId())) {
            // 抛出权限异常，交由全局异常处理转为 403
            throw new AccessDeniedException("权限不足：无权更新此实验");
        }
        experiment.setName(requestDTO.getName());
        experiment.setDescription(requestDTO.getDescription());
        experiment.setStatus(requestDTO.getStatus() != null ? requestDTO.getStatus() : experiment.getStatus());
        experiment.setStartTime(requestDTO.getStartTime());
        experiment.setEndTime(requestDTO.getEndTime());

        Experiment updatedExperiment = experimentRepository.save(experiment);

        return convertToDTO(updatedExperiment);
    }

    @Override
    @Transactional
    public void deleteExperiment(String id) {
        // 幂等：不存在则认为已删除
        if (!experimentRepository.existsById(id)) {
            return;
        }

        // 先删除与该实验相关的任务分配记录和任务，再删实验，避免外键约束失败
        var tasks = experimentTaskRepository.findByExperimentId(id);
        if (tasks != null && !tasks.isEmpty()) {
            for (var task : tasks) {
                try {
                    experimentAssignmentRepository.deleteByTaskId(task.getId());
                } catch (Exception ignored) {
                    // 分配记录可能不存在，忽略
                }
            }
            experimentTaskRepository.deleteByExperimentId(id);
        }

        experimentRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ExperimentDTO publishExperiment(String id) {
        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("实验不存在"));
        // 仅创建者可以发布：从安全上下文获取当前用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : null;
        if (username == null || "anonymousUser".equals(username)) {
            throw new AccessDeniedException("未认证或权限不足：无权发布此实验");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!experiment.getCreatorId().equals(user.getId())) {
            throw new AccessDeniedException("权限不足：无权发布此实验");
        }
        experiment.setStatus(Experiment.ExperimentStatus.PUBLISHED);
        Experiment publishedExperiment = experimentRepository.save(experiment);

    

        return convertToDTO(publishedExperiment);
    }

    @Override
    @Transactional
    public ExperimentDTO unpublishExperiment(String id) {
        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("实验不存在"));
        // 仅创建者可以取消发布：从安全上下文获取当前用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : null;
        if (username == null || "anonymousUser".equals(username)) {
            throw new AccessDeniedException("未认证或权限不足：无权取消发布此实验");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!experiment.getCreatorId().equals(user.getId())) {
            throw new AccessDeniedException("权限不足：无权取消发布此实验");
        }
        experiment.setStatus(Experiment.ExperimentStatus.DRAFT);
        Experiment unpublishedExperiment = experimentRepository.save(experiment);

        return convertToDTO(unpublishedExperiment);
    }

    /**
     * 将实验实体转换为DTO
     * 
     * @param experiment  实验实体
     * @return 实验DTO
     */
    private ExperimentDTO convertToDTO(Experiment experiment) {
        ExperimentDTO dto = new ExperimentDTO();
        dto.setId(experiment.getId());
        dto.setCreator_Id(experiment.getCreatorId());
        dto.setName(experiment.getName());
        dto.setDescription(experiment.getDescription());
        dto.setStatus(experiment.getStatus());
        dto.setStartTime(experiment.getStartTime());
        dto.setEndTime(experiment.getEndTime());
        return dto;
    }
}
