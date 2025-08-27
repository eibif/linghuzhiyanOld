package org.linghu.mybackend.service.impl;

import org.linghu.mybackend.domain.Question;
import org.linghu.mybackend.domain.Question.QuestionType;
import org.linghu.mybackend.domain.User;
import org.linghu.mybackend.dto.QuestionDTO;
import org.linghu.mybackend.dto.QuestionRequestDTO;
import org.linghu.mybackend.utils.JsonUtils;
import org.linghu.mybackend.repository.QuestionRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.linghu.mybackend.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题库管理服务实现类
 */
@Service
public class QuestionServiceImpl implements QuestionService {
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final SimpleDateFormat dateFormat;

    @Autowired
    public QuestionServiceImpl(
            QuestionRepository questionRepository,
            UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @Override
    @Transactional
    public QuestionDTO createQuestion(QuestionRequestDTO requestDTO, String username) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Question question = Question.builder()
                .id(UUID.randomUUID().toString())
                .questionType(requestDTO.getQuestionType())
                .content(requestDTO.getContent())
                .options(convertObjectToJson(requestDTO.getOptions()))
                .answer(convertObjectToJson(requestDTO.getAnswer()))
                .explanation(requestDTO.getExplanation())
                .tags(requestDTO.getTags())
                .score(requestDTO.getScore())
                .createdAt(new Date())
                .build();

        Question savedQuestion = questionRepository.save(question);
        return convertToDTO(savedQuestion);
    }

    @Override
    public QuestionDTO getQuestionById(String id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));
        return convertToDTO(question);
    }

    @Override
    @Transactional
    public QuestionDTO updateQuestion(String id, QuestionRequestDTO requestDTO) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));

        question.setQuestionType(requestDTO.getQuestionType());
        question.setContent(requestDTO.getContent());
        question.setOptions(convertObjectToJson(requestDTO.getOptions()));
        question.setAnswer(convertObjectToJson(requestDTO.getAnswer()));
        question.setExplanation(requestDTO.getExplanation());
        question.setTags(requestDTO.getTags());
        question.setScore(requestDTO.getScore());
        question.setUpdatedAt(new Date());

        Question updatedQuestion = questionRepository.save(question);
        return convertToDTO(updatedQuestion);
    }

    @Override
    @Transactional
    public void deleteQuestion(String id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));
        questionRepository.delete(question);
    }

    @Override
    public Page<QuestionDTO> getQuestions(QuestionType type, String keyword, String tags, Pageable pageable) {
        Page<Question> questionPage;

        if (type != null && StringUtils.hasText(keyword) && StringUtils.hasText(tags)) {
            // 组合查询: 按类型、关键词和标签
            Set<String> tagSet = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());

            List<Question> questions = questionRepository.findByTagsIn(tagSet);
            questions = questions.stream()
                    .filter(q -> q.getQuestionType() == type && q.getContent().contains(keyword))
                    .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), questions.size());
            questionPage = new PageImpl<>(
                    questions.subList(start, end),
                    pageable,
                    questions.size());
        } else if (type != null) {
            // 只按类型查询
            questionPage = questionRepository.findByQuestionType(type, pageable);
        } else if (StringUtils.hasText(keyword)) {
            // 只按关键词查询
            List<Question> questions = questionRepository.findByContentContaining(keyword);

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), questions.size());
            questionPage = new PageImpl<>(
                    questions.subList(start, end),
                    pageable,
                    questions.size());
        } else if (StringUtils.hasText(tags)) {
            // 只按标签查询
            Set<String> tagSet = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());

            List<Question> questions = questionRepository.findByTagsIn(tagSet);

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), questions.size());
            questionPage = new PageImpl<>(
                    questions.subList(start, end),
                    pageable,
                    questions.size());
        } else {
            // 无条件查询所有
            questionPage = questionRepository.findAll(pageable);
        }

        return questionPage.map(this::convertToDTO);
    }

    @Override
    public List<QuestionDTO> searchQuestions(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new RuntimeException("搜索关键词不能为空");
        }

        List<Question> questions = questionRepository.findByContentContaining(keyword);
        return questions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionType> getQuestionTypes() {
        return Arrays.asList(QuestionType.values());
    }

    @Override
    public Set<String> getAllTags() {
        Set<String> allTags = new HashSet<>();

        List<Question> questions = questionRepository.findAll();
        for (Question question : questions) {
            if (StringUtils.hasText(question.getTags())) {
                String[] tags = question.getTags().split(",");
                for (String tag : tags) {
                    String trimmedTag = tag.trim();
                    if (StringUtils.hasText(trimmedTag)) {
                        allTags.add(trimmedTag);
                    }
                }
            }
        }

        return allTags;
    }

    @Override
    public Page<QuestionDTO> getQuestionsByTag(String tag, Pageable pageable) {
        if (!StringUtils.hasText(tag)) {
            throw new RuntimeException("标签不能为空");
        }

        List<Question> questions = questionRepository.findByTagsContaining(tag);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), questions.size());
        Page<Question> questionPage = new PageImpl<>(
                questions.subList(start, end),
                pageable,
                questions.size());

        return questionPage.map(this::convertToDTO);
    }

    @Override
    public Page<QuestionDTO> getQuestionsByType(QuestionType type, Pageable pageable) {
        if (type == null) {
            throw new RuntimeException("题目类型不能为空");
        }

        Page<Question> questionPage = questionRepository.findByQuestionType(type, pageable);
        return questionPage.map(this::convertToDTO);
    }

    @Override
    public List<QuestionDTO> getQuestionsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Question> questions = questionRepository.findByIdIn(ids);
        return questions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将Question实体转换为QuestionDTO
     *
     * @param question 题目实体
     * @return 题目DTO
     */
    private QuestionDTO convertToDTO(Question question) {
        QuestionDTO dto = QuestionDTO.builder()
                .id(question.getId())
                .questionType(question.getQuestionType())
                .content(question.getContent())
                .options(convertJsonToObject(question.getOptions()))
                .answer(convertJsonToObject(question.getAnswer()))
                .explanation(question.getExplanation())
                .tags(question.getTags())
                .createdAt(formatDate(question.getCreatedAt()))
                .updatedAt(formatDate(question.getUpdatedAt()))
                .build();

        return dto;
    }

    /**
     * 将对象转换为JSON字符串
     *
     * @param object 要转换的对象
     * @return JSON字符串
     */
    private String convertObjectToJson(Object object) {
        if (object == null) {
            return null;
        }

        String jsonString = JsonUtils.toJsonString(object);
        if (jsonString == null) {
            throw new RuntimeException("对象转换为JSON失败");
        }
        return jsonString;
    }

    /**
     * 将JSON字符串转换为对象
     *
     * @param json JSON字符串
     * @return 转换后的对象
     */
    private Object convertJsonToObject(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }

        Object result = JsonUtils.parseObject(json, Object.class);
        if (result == null) {
            throw new RuntimeException("JSON转换为对象失败");
        }
        return result;
    }

    /**
     * 格式化日期
     *
     * @param date 日期
     * @return 格式化后的日期字符串
     */
    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }

        return dateFormat.format(date);
    }
}
