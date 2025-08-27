package org.linghu.mybackend.service.impl;

import org.linghu.mybackend.domain.Experiment;
import org.linghu.mybackend.domain.User;
import org.linghu.mybackend.dto.ExperimentDTO;
import org.linghu.mybackend.dto.ExperimentRequestDTO;
import org.linghu.mybackend.repository.ExperimentRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.linghu.mybackend.service.ExperimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 实验管理服务实现类
 */
@Service
public class ExperimentServiceImpl implements ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final UserRepository userRepository;

    @Autowired
    public ExperimentServiceImpl(ExperimentRepository experimentRepository, UserRepository userRepository) {
        this.experimentRepository = experimentRepository;
        this.userRepository = userRepository;
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
            throw new RuntimeException("无权限更新此实验");
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
        if (!experimentRepository.existsById(id)) {
            throw new RuntimeException("实验不存在");
        }
        experimentRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ExperimentDTO publishExperiment(String id) {
        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("实验不存在"));

        experiment.setStatus(Experiment.ExperimentStatus.PUBLISHED);
        Experiment publishedExperiment = experimentRepository.save(experiment);

    

        return convertToDTO(publishedExperiment);
    }

    @Override
    @Transactional
    public ExperimentDTO unpublishExperiment(String id) {
        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("实验不存在"));

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
