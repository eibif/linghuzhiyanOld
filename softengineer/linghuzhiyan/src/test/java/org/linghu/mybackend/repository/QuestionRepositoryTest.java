package org.linghu.mybackend.repository;

import org.junit.jupiter.api.Test;
import org.linghu.mybackend.domain.Question;
import org.linghu.mybackend.domain.Question.QuestionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QuestionRepository 测试类
 */
@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = {"/schema.sql", "/data.sql"})
class QuestionRepositoryTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    void findByQuestionType_WithExistingType_ShouldReturnQuestions() {
        // When
        List<Question> questions = questionRepository.findByQuestionType(QuestionType.SINGLE_CHOICE);

        // Then
        assertNotNull(questions);
        assertEquals(2, questions.size());
        assertTrue(questions.stream().allMatch(q -> QuestionType.SINGLE_CHOICE.equals(q.getQuestionType())));
    }

    @Test
    void findByQuestionType_WithNonPopularType_ShouldReturnEmptyList() {
        // When
        List<Question> questions = questionRepository.findByQuestionType(QuestionType.QA);

        // Then
        assertNotNull(questions);
        assertTrue(questions.isEmpty());
    }

    @Test
    void findByQuestionTypeWithPageable_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Question> questions = questionRepository.findByQuestionType(QuestionType.SINGLE_CHOICE, pageable);

        // Then
        assertNotNull(questions);
        assertEquals(1, questions.getContent().size());
        assertEquals(2, questions.getTotalElements());
        assertEquals(2, questions.getTotalPages());
    }

    @Test
    void findByTagsContaining_WithExistingTag_ShouldReturnQuestions() {
        // When
        List<Question> questions = questionRepository.findByTagsContaining("math");

        // Then
        assertNotNull(questions);
        assertEquals(1, questions.size());
        assertTrue(questions.stream().allMatch(q -> q.getTags().contains("math")));
    }

    @Test
    void findByTagsContaining_WithNonExistingTag_ShouldReturnEmptyList() {
        // When
        List<Question> questions = questionRepository.findByTagsContaining("nonexistent");

        // Then
        assertNotNull(questions);
        assertTrue(questions.isEmpty());
    }

    @Test
    void findByContentContaining_WithExistingContent_ShouldReturnQuestions() {
        // When
        List<Question> questions = questionRepository.findByContentContaining("What is");

        // Then
        assertNotNull(questions);
        assertTrue(questions.size() > 0);
        assertTrue(questions.stream().allMatch(q -> q.getContent().contains("What is")));
    }

    @Test
    void findByContentContaining_WithNonExistingContent_ShouldReturnEmptyList() {
        // When
        List<Question> questions = questionRepository.findByContentContaining("nonexistent content");

        // Then
        assertNotNull(questions);
        assertTrue(questions.isEmpty());
    }

    @Test
    void findByIdIn_WithExistingIds_ShouldReturnQuestions() {
        // Given
        List<String> ids = Arrays.asList("q1", "q2", "q3");

        // When
        List<Question> questions = questionRepository.findByIdIn(ids);

        // Then
        assertNotNull(questions);
        assertEquals(3, questions.size());
        assertTrue(questions.stream().allMatch(q -> ids.contains(q.getId())));
    }

    @Test
    void findByIdIn_WithNonExistingIds_ShouldReturnEmptyList() {
        // Given
        List<String> ids = Arrays.asList("nonexistent1", "nonexistent2");

        // When
        List<Question> questions = questionRepository.findByIdIn(ids);

        // Then
        assertNotNull(questions);
        assertTrue(questions.isEmpty());
    }

    @Test
    void findByIdIn_WithMixedIds_ShouldReturnOnlyExistingQuestions() {
        // Given
        List<String> ids = Arrays.asList("q1", "nonexistent", "q2");

        // When
        List<Question> questions = questionRepository.findByIdIn(ids);

        // Then
        assertNotNull(questions);
        assertEquals(2, questions.size());
        assertTrue(questions.stream().allMatch(q -> Arrays.asList("q1", "q2").contains(q.getId())));
    }

    @Test
    void countByQuestionType_WithExistingType_ShouldReturnCorrectCount() {
        // When
        long count = questionRepository.countByQuestionType(QuestionType.SINGLE_CHOICE);

        // Then
        assertEquals(2, count);
    }

    @Test
    void countByQuestionType_WithNonPopularType_ShouldReturnZero() {
        // When
        long count = questionRepository.countByQuestionType(QuestionType.QA);

        // Then
        assertEquals(0, count);
    }

    @Test
    void save_WithValidQuestion_ShouldSaveSuccessfully() {
        // Given
        Date now = new Date();
        Question newQuestion = Question.builder()
                .id(UUID.randomUUID().toString())
                .content("New question content")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .tags("java,algorithm")
                .options("{\"A\":\"Option A\",\"B\":\"Option B\"}")
                .answer("{\"correct\":[\"A\"]}")
                .explanation("This is an explanation")
                .score(new BigDecimal("100"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        // When
        Question saved = questionRepository.save(newQuestion);

        // Then
        assertNotNull(saved);
        assertEquals("New question content", saved.getContent());
        assertEquals(QuestionType.MULTIPLE_CHOICE, saved.getQuestionType());
        assertEquals("java,algorithm", saved.getTags());
        assertEquals(new BigDecimal("100"), saved.getScore());
    }

    @Test
    void findById_WithExistingId_ShouldReturnQuestion() {
        // When
        Optional<Question> found = questionRepository.findById("q1");

        // Then
        assertTrue(found.isPresent());
        assertEquals("q1", found.get().getId());
        assertNotNull(found.get().getContent());
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // When
        Optional<Question> found = questionRepository.findById("nonexistent");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void deleteById_WithExistingId_ShouldDeleteSuccessfully() {
        // Given
        String idToDelete = "q1";

        // When
        questionRepository.deleteById(idToDelete);

        // Then
        Optional<Question> deleted = questionRepository.findById(idToDelete);
        assertFalse(deleted.isPresent());
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // When
        long count = questionRepository.count();

        // Then
        assertEquals(4, count);
    }

    @Test
    void save_UpdateExistingQuestion_ShouldUpdateSuccessfully() {
        // Given - 先获取现有的问题
        Optional<Question> existing = questionRepository.findById("q1");
        assertTrue(existing.isPresent());
        
        Question question = existing.get();
        question.setContent("Updated content");
        question.setScore(new BigDecimal("150"));
        question.setExplanation("Updated explanation");

        // When
        Question updated = questionRepository.save(question);

        // Then
        assertNotNull(updated);
        assertEquals("Updated content", updated.getContent());
        assertEquals(new BigDecimal("150"), updated.getScore());
        assertEquals("Updated explanation", updated.getExplanation());
    }

    @Test
    void findAll_ShouldReturnAllQuestions() {
        // When
        List<Question> questions = questionRepository.findAll();

        // Then
        assertNotNull(questions);
        assertEquals(4, questions.size());
    }

    @Test
    void existsById_WithExistingId_ShouldReturnTrue() {
        // When
        boolean exists = questionRepository.existsById("q1");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsById_WithNonExistingId_ShouldReturnFalse() {
        // When
        boolean exists = questionRepository.existsById("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void deleteAll_ShouldDeleteAllQuestions() {
        // When
        questionRepository.deleteAll();

        // Then
        long count = questionRepository.count();
        assertEquals(0, count);
    }
}
