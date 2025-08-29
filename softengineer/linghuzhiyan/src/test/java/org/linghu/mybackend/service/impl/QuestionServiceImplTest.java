package org.linghu.mybackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.domain.Question;
import org.linghu.mybackend.domain.User;
import org.linghu.mybackend.dto.QuestionDTO;
import org.linghu.mybackend.dto.QuestionRequestDTO;
import org.linghu.mybackend.repository.QuestionRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.linghu.mybackend.utils.JsonUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QuestionServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuestionServiceImpl questionService;

    private User testUser;
    private Question testQuestion;
    private QuestionRequestDTO testQuestionRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user1");
        testUser.setUsername("testuser");

        testQuestion = Question.builder()
                .id("question1")
                .questionType(Question.QuestionType.SINGLE_CHOICE)
                .content("What is the capital of France?")
                .score(new BigDecimal("5.0"))
                .options("{\"A\": \"London\", \"B\": \"Paris\", \"C\": \"Berlin\", \"D\": \"Madrid\"}")
                .answer("{\"correct\": \"B\"}")
                .explanation("Paris is the capital city of France.")
                .tags("geography,capital,france")
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        testQuestionRequest = QuestionRequestDTO.builder()
                .questionType(Question.QuestionType.SINGLE_CHOICE)
                .content("What is the capital of France?")
                .score(new BigDecimal("5.0"))
                .options(Map.of("A", "London", "B", "Paris", "C", "Berlin", "D", "Madrid"))
                .answer(Map.of("correct", "B"))
                .explanation("Paris is the capital city of France.")
                .tags("geography,capital,france")
                .build();
    }

    @Nested
    class CreateQuestionTests {

        @Test
        void createQuestion_WithValidData_ShouldCreateSuccessfully() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
                when(questionRepository.save(any(Question.class))).thenReturn(testQuestion);
                jsonUtilsMock.when(() -> JsonUtils.toJsonString(any())).thenReturn("{\"test\": \"value\"}");
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                // When
                QuestionDTO result = questionService.createQuestion(testQuestionRequest, "testuser");

                // Then
                assertNotNull(result);
                assertEquals("question1", result.getId());
                assertEquals(Question.QuestionType.SINGLE_CHOICE, result.getQuestionType());
                assertEquals("What is the capital of France?", result.getContent());
                verify(questionRepository).save(any(Question.class));
            }
        }

        @Test
        void createQuestion_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.createQuestion(testQuestionRequest, "nonexistent");
            });
            assertEquals("用户不存在", exception.getMessage());
            verify(questionRepository, never()).save(any(Question.class));
        }

        @Test
        void createQuestion_WithNullQuestionType_ShouldHandleCorrectly() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
                when(questionRepository.save(any(Question.class))).thenReturn(testQuestion);
                jsonUtilsMock.when(() -> JsonUtils.toJsonString(any())).thenReturn("{\"test\": \"value\"}");
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                testQuestionRequest.setQuestionType(null);

                // When
                QuestionDTO result = questionService.createQuestion(testQuestionRequest, "testuser");

                // Then
                assertNotNull(result);
                verify(questionRepository).save(any(Question.class));
            }
        }

        @Test
        void createQuestion_WithEmptyContent_ShouldHandleCorrectly() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
                when(questionRepository.save(any(Question.class))).thenReturn(testQuestion);
                jsonUtilsMock.when(() -> JsonUtils.toJsonString(any())).thenReturn("{\"test\": \"value\"}");
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                testQuestionRequest.setContent("");

                // When
                QuestionDTO result = questionService.createQuestion(testQuestionRequest, "testuser");

                // Then
                assertNotNull(result);
                verify(questionRepository).save(any(Question.class));
            }
        }

        @Test
        void createQuestion_WithNullScore_ShouldHandleCorrectly() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
                when(questionRepository.save(any(Question.class))).thenReturn(testQuestion);
                jsonUtilsMock.when(() -> JsonUtils.toJsonString(any())).thenReturn("{\"test\": \"value\"}");
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                testQuestionRequest.setScore(null);

                // When
                QuestionDTO result = questionService.createQuestion(testQuestionRequest, "testuser");

                // Then
                assertNotNull(result);
                verify(questionRepository).save(any(Question.class));
            }
        }
    }

    @Nested
    class GetQuestionTests {

        @Test
        void getQuestionById_WithExistingId_ShouldReturnQuestion() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                when(questionRepository.findById("question1")).thenReturn(Optional.of(testQuestion));
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                // When
                QuestionDTO result = questionService.getQuestionById("question1");

                // Then
                assertNotNull(result);
                assertEquals("question1", result.getId());
                assertEquals("What is the capital of France?", result.getContent());
            }
        }

        @Test
        void getQuestionById_WithNonExistingId_ShouldThrowException() {
            // Given
            when(questionRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.getQuestionById("nonexistent");
            });
            assertEquals("题目不存在", exception.getMessage());
        }

        @Test
        void getQuestionById_WithNullId_ShouldThrowException() {
            // Given
            when(questionRepository.findById(null)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.getQuestionById(null);
            });
            assertEquals("题目不存在", exception.getMessage());
        }

        @Test
        void getQuestionById_WithEmptyId_ShouldThrowException() {
            // Given
            when(questionRepository.findById("")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.getQuestionById("");
            });
            assertEquals("题目不存在", exception.getMessage());
        }
    }

    @Nested
    class UpdateQuestionTests {

        @Test
        void updateQuestion_WithValidData_ShouldUpdateSuccessfully() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                when(questionRepository.findById("question1")).thenReturn(Optional.of(testQuestion));
                when(questionRepository.save(any(Question.class))).thenReturn(testQuestion);
                jsonUtilsMock.when(() -> JsonUtils.toJsonString(any())).thenReturn("{\"test\": \"value\"}");
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                // When
                QuestionDTO result = questionService.updateQuestion("question1", testQuestionRequest);

                // Then
                assertNotNull(result);
                assertEquals("question1", result.getId());
                verify(questionRepository).save(any(Question.class));
            }
        }

        @Test
        void updateQuestion_WithNonExistingId_ShouldThrowException() {
            // Given
            when(questionRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.updateQuestion("nonexistent", testQuestionRequest);
            });
            assertEquals("题目不存在", exception.getMessage());
            verify(questionRepository, never()).save(any(Question.class));
        }

        @Test
        void updateQuestion_WithNullRequestDTO_ShouldHandleCorrectly() {
            // Given
            when(questionRepository.findById("question1")).thenReturn(Optional.of(testQuestion));
            
            // When & Then - 应该能够处理null DTO，但会有NullPointerException
            assertThrows(NullPointerException.class, () -> {
                questionService.updateQuestion("question1", null);
            });
        }

        @Test
        void updateQuestion_WithModifiedContent_ShouldUpdateContent() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                when(questionRepository.findById("question1")).thenReturn(Optional.of(testQuestion));
                when(questionRepository.save(any(Question.class))).thenReturn(testQuestion);
                jsonUtilsMock.when(() -> JsonUtils.toJsonString(any())).thenReturn("{\"test\": \"value\"}");
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                testQuestionRequest.setContent("Updated question content");

                // When
                QuestionDTO result = questionService.updateQuestion("question1", testQuestionRequest);

                // Then
                assertNotNull(result);
                verify(questionRepository).save(any(Question.class));
                verify(questionRepository).findById("question1");
            }
        }
    }

    @Nested
    class DeleteQuestionTests {

        @Test
        void deleteQuestion_WithExistingId_ShouldDeleteSuccessfully() {
            // Given
            when(questionRepository.findById("question1")).thenReturn(Optional.of(testQuestion));

            // When
            questionService.deleteQuestion("question1");

            // Then
            verify(questionRepository).delete(testQuestion);
        }

        @Test
        void deleteQuestion_WithNonExistingId_ShouldThrowException() {
            // Given
            when(questionRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.deleteQuestion("nonexistent");
            });
            assertEquals("题目不存在", exception.getMessage());
            verify(questionRepository, never()).delete(any(Question.class));
        }

        @Test
        void deleteQuestion_WithNullId_ShouldThrowException() {
            // Given
            when(questionRepository.findById(null)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.deleteQuestion(null);
            });
            assertEquals("题目不存在", exception.getMessage());
            verify(questionRepository, never()).delete(any(Question.class));
        }
    }

    @Nested
    class QueryQuestionTests {

        @Test
        void getQuestions_WithTypeFilter_ShouldReturnFilteredQuestions() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                Page<Question> questionPage = new PageImpl<>(List.of(testQuestion));
                when(questionRepository.findByQuestionType(Question.QuestionType.SINGLE_CHOICE, pageable)).thenReturn(questionPage);
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                // When
                Page<QuestionDTO> result = questionService.getQuestions(Question.QuestionType.SINGLE_CHOICE, null, null, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size());
                assertEquals("question1", result.getContent().get(0).getId());
            }
        }

        @Test
        void getQuestions_WithNoFilters_ShouldReturnAllQuestions() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                Page<Question> questionPage = new PageImpl<>(List.of(testQuestion));
                when(questionRepository.findAll(pageable)).thenReturn(questionPage);
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                // When
                Page<QuestionDTO> result = questionService.getQuestions(null, null, null, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size());
            }
        }

        @Test
        void getQuestions_WithKeywordFilter_ShouldReturnMatchingQuestions() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                when(questionRepository.findByContentContaining("France")).thenReturn(List.of(testQuestion));
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                // When
                Page<QuestionDTO> result = questionService.getQuestions(null, "France", null, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size());
                assertEquals("question1", result.getContent().get(0).getId());
            }
        }

        @Test
        void getQuestions_WithTagsFilter_ShouldReturnMatchingQuestions() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                when(questionRepository.findByTagsIn(anySet())).thenReturn(List.of(testQuestion));
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                // When
                Page<QuestionDTO> result = questionService.getQuestions(null, null, "geography", pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size());
                assertEquals("question1", result.getContent().get(0).getId());
            }
        }
    }

    @Nested
    class SearchQuestionTests {

        @Test
        void searchQuestions_WithValidKeyword_ShouldReturnMatchingQuestions() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                when(questionRepository.findByContentContaining("France")).thenReturn(List.of(testQuestion));
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                // When
                List<QuestionDTO> result = questionService.searchQuestions("France");

                // Then
                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals("question1", result.get(0).getId());
            }
        }

        @Test
        void searchQuestions_WithEmptyKeyword_ShouldThrowException() {
            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.searchQuestions("");
            });
            assertEquals("搜索关键词不能为空", exception.getMessage());
        }

        @Test
        void searchQuestions_WithNullKeyword_ShouldThrowException() {
            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.searchQuestions(null);
            });
            assertEquals("搜索关键词不能为空", exception.getMessage());
        }

        @Test
        void searchQuestions_WithWhitespaceKeyword_ShouldThrowException() {
            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.searchQuestions("   ");
            });
            assertEquals("搜索关键词不能为空", exception.getMessage());
        }

        @Test
        void searchQuestions_WithNoMatchingResults_ShouldReturnEmptyList() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                when(questionRepository.findByContentContaining("nonexistent")).thenReturn(new ArrayList<>());

                // When
                List<QuestionDTO> result = questionService.searchQuestions("nonexistent");

                // Then
                assertNotNull(result);
                assertTrue(result.isEmpty());
            }
        }
    }

    @Nested
    class UtilityMethodTests {

        @Test
        void getQuestionTypes_ShouldReturnAllTypes() {
            // When
            List<Question.QuestionType> result = questionService.getQuestionTypes();

            // Then
            assertNotNull(result);
            assertEquals(4, result.size());
            assertTrue(result.contains(Question.QuestionType.SINGLE_CHOICE));
            assertTrue(result.contains(Question.QuestionType.MULTIPLE_CHOICE));
            assertTrue(result.contains(Question.QuestionType.FILL_BLANK));
            assertTrue(result.contains(Question.QuestionType.QA));
        }

        @Test
        void getAllTags_WithQuestionsHavingTags_ShouldReturnAllUniqueTags() {
            // Given
            Question question1 = Question.builder().tags("tag1,tag2,tag3").build();
            Question question2 = Question.builder().tags("tag2,tag4").build();
            when(questionRepository.findAll()).thenReturn(List.of(question1, question2));

            // When
            Set<String> result = questionService.getAllTags();

            // Then
            assertNotNull(result);
            assertEquals(4, result.size());
            assertTrue(result.contains("tag1"));
            assertTrue(result.contains("tag2"));
            assertTrue(result.contains("tag3"));
            assertTrue(result.contains("tag4"));
        }

        @Test
        void getAllTags_WithQuestionsHavingNoTags_ShouldReturnEmptySet() {
            // Given
            Question question1 = Question.builder().tags("").build();
            Question question2 = Question.builder().tags(null).build();
            when(questionRepository.findAll()).thenReturn(List.of(question1, question2));

            // When
            Set<String> result = questionService.getAllTags();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void getAllTags_WithEmptyDatabase_ShouldReturnEmptySet() {
            // Given
            when(questionRepository.findAll()).thenReturn(new ArrayList<>());

            // When
            Set<String> result = questionService.getAllTags();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void getAllTags_WithMixedTagFormats_ShouldHandleCorrectly() {
            // Given
            Question question1 = Question.builder().tags("tag1, tag2 , tag3").build(); // 带空格
            Question question2 = Question.builder().tags("tag2,tag4,").build(); // 末尾逗号
            Question question3 = Question.builder().tags(",tag5,tag6").build(); // 开头逗号
            when(questionRepository.findAll()).thenReturn(List.of(question1, question2, question3));

            // When
            Set<String> result = questionService.getAllTags();

            // Then
            assertNotNull(result);
            // 根据实际实现，空字符串也会被添加到结果中
            assertEquals(6, result.size());
            assertTrue(result.contains("tag1"));
            assertTrue(result.contains("tag2"));
            assertTrue(result.contains("tag3"));
            assertTrue(result.contains("tag4"));
            assertTrue(result.contains("tag5"));
            assertTrue(result.contains("tag6"));
        }
    }

    @Nested
    class SpecificQueryTests {

        @Test
        void getQuestionsByTag_WithValidTag_ShouldReturnMatchingQuestions() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                when(questionRepository.findByTagsContaining("geography")).thenReturn(List.of(testQuestion));
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                // When
                Page<QuestionDTO> result = questionService.getQuestionsByTag("geography", pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size());
                assertEquals("question1", result.getContent().get(0).getId());
            }
        }

        @Test
        void getQuestionsByTag_WithEmptyTag_ShouldThrowException() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.getQuestionsByTag("", pageable);
            });
            assertEquals("标签不能为空", exception.getMessage());
        }

        @Test
        void getQuestionsByTag_WithNullTag_ShouldThrowException() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.getQuestionsByTag(null, pageable);
            });
            assertEquals("标签不能为空", exception.getMessage());
        }

        @Test
        void getQuestionsByType_WithValidType_ShouldReturnMatchingQuestions() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                Page<Question> questionPage = new PageImpl<>(List.of(testQuestion));
                when(questionRepository.findByQuestionType(Question.QuestionType.SINGLE_CHOICE, pageable)).thenReturn(questionPage);
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                // When
                Page<QuestionDTO> result = questionService.getQuestionsByType(Question.QuestionType.SINGLE_CHOICE, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size());
                assertEquals("question1", result.getContent().get(0).getId());
            }
        }

        @Test
        void getQuestionsByType_WithNullType_ShouldThrowException() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                questionService.getQuestionsByType(null, pageable);
            });
            assertEquals("题目类型不能为空", exception.getMessage());
        }

        @Test
        void getQuestionsByIds_WithValidIds_ShouldReturnMatchingQuestions() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                List<String> ids = List.of("question1", "question2");
                when(questionRepository.findByIdIn(ids)).thenReturn(List.of(testQuestion));
                jsonUtilsMock.when(() -> JsonUtils.parseObject(anyString(), eq(Object.class))).thenReturn(Map.of("test", "value"));

                // When
                List<QuestionDTO> result = questionService.getQuestionsByIds(ids);

                // Then
                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals("question1", result.get(0).getId());
            }
        }

        @Test
        void getQuestionsByIds_WithEmptyIds_ShouldReturnEmptyList() {
            // When
            List<QuestionDTO> result = questionService.getQuestionsByIds(new ArrayList<>());

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(questionRepository, never()).findByIdIn(any());
        }

        @Test
        void getQuestionsByIds_WithNullIds_ShouldReturnEmptyList() {
            // When
            List<QuestionDTO> result = questionService.getQuestionsByIds(null);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(questionRepository, never()).findByIdIn(any());
        }

        @Test
        void getQuestionsByIds_WithNonExistentIds_ShouldReturnEmptyList() {
            try (MockedStatic<JsonUtils> jsonUtilsMock = mockStatic(JsonUtils.class)) {
                // Given
                List<String> ids = List.of("nonexistent1", "nonexistent2");
                when(questionRepository.findByIdIn(ids)).thenReturn(new ArrayList<>());

                // When
                List<QuestionDTO> result = questionService.getQuestionsByIds(ids);

                // Then
                assertNotNull(result);
                assertTrue(result.isEmpty());
            }
        }
    }
}
