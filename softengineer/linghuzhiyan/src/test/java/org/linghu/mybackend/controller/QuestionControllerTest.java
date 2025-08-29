package org.linghu.mybackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.domain.Question.QuestionType;
import org.linghu.mybackend.dto.PageResult;
import org.linghu.mybackend.dto.QuestionDTO;
import org.linghu.mybackend.dto.QuestionRequestDTO;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.service.QuestionService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QuestionController 单元测试 - 使用纯单元测试方式
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("题库管理控制器测试")
class QuestionControllerTest {

    @Mock
    private QuestionService questionService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private QuestionController questionController;

    private QuestionDTO sampleQuestionDTO;
    private QuestionRequestDTO questionRequestDTO;
    private List<QuestionDTO> questionList;

    @BeforeEach
    void setUp() {
        // 移除全局的 userDetails stubbing，改为在需要时单独设置

        // 创建测试题目DTO
        sampleQuestionDTO = QuestionDTO.builder()
                .id("q123")
                .content("测试题目内容")
                .questionType(QuestionType.SINGLE_CHOICE)
                .tags("测试,单选")
                .createdAt("2023-12-01T10:00:00")
                .updatedAt("2023-12-01T10:00:00")
                .build();

        // 创建测试题目请求
        questionRequestDTO = QuestionRequestDTO.builder()
                .content("测试题目内容")
                .questionType(QuestionType.SINGLE_CHOICE)
                .tags("测试,单选")
                .score(new BigDecimal("10"))
                .build();

        // 创建题目列表
        questionList = Arrays.asList(sampleQuestionDTO);
    }

    @Nested
    @DisplayName("创建题目测试")
    class CreateQuestionTests {

        @Test
        @DisplayName("成功创建题目")
        void shouldCreateQuestionSuccessfully() {
            // given
            when(userDetails.getUsername()).thenReturn("teacher123");
            when(questionService.createQuestion(questionRequestDTO, "teacher123")).thenReturn(sampleQuestionDTO);

            // when
            Result<QuestionDTO> response = questionController.createQuestion(questionRequestDTO, userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(sampleQuestionDTO);

            verify(questionService).createQuestion(questionRequestDTO, "teacher123");
        }

        @Test
        @DisplayName("创建题目时参数无效")
        void shouldHandleInvalidQuestionRequest() {
            // given
            when(userDetails.getUsername()).thenReturn("teacher123");
            QuestionRequestDTO invalidRequest = new QuestionRequestDTO();
            when(questionService.createQuestion(invalidRequest, "teacher123"))
                    .thenThrow(new RuntimeException("题目参数无效"));

            // when & then
            try {
                questionController.createQuestion(invalidRequest, userDetails);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("题目参数无效");
            }

            verify(questionService).createQuestion(invalidRequest, "teacher123");
        }

        @Test
        @DisplayName("创建题目时服务异常")
        void shouldHandleServiceExceptionWhenCreating() {
            // given
            when(userDetails.getUsername()).thenReturn("teacher123");
            when(questionService.createQuestion(questionRequestDTO, "teacher123"))
                    .thenThrow(new RuntimeException("创建题目失败"));

            // when & then
            try {
                questionController.createQuestion(questionRequestDTO, userDetails);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("创建题目失败");
            }

            verify(questionService).createQuestion(questionRequestDTO, "teacher123");
        }
    }

    @Nested
    @DisplayName("删除题目测试")
    class DeleteQuestionTests {

        @Test
        @DisplayName("成功删除题目")
        void shouldDeleteQuestionSuccessfully() {
            // given
            doNothing().when(questionService).deleteQuestion("q123");

            // when
            Result<Void> response = questionController.deleteQuestion("q123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNull();

            verify(questionService).deleteQuestion("q123");
        }

        @Test
        @DisplayName("删除不存在的题目")
        void shouldHandleDeleteNonexistentQuestion() {
            // given
            doThrow(new RuntimeException("题目未找到")).when(questionService).deleteQuestion("nonexistent");

            // when & then
            try {
                questionController.deleteQuestion("nonexistent");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("题目未找到");
            }

            verify(questionService).deleteQuestion("nonexistent");
        }

        @Test
        @DisplayName("删除题目时服务异常")
        void shouldHandleServiceExceptionWhenDeleting() {
            // given
            doThrow(new RuntimeException("删除题目失败")).when(questionService).deleteQuestion("q123");

            // when & then
            try {
                questionController.deleteQuestion("q123");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("删除题目失败");
            }

            verify(questionService).deleteQuestion("q123");
        }
    }

    @Nested
    @DisplayName("更新题目测试")
    class UpdateQuestionTests {

        @Test
        @DisplayName("成功更新题目")
        void shouldUpdateQuestionSuccessfully() {
            // given
            QuestionDTO updatedQuestion = QuestionDTO.builder()
                    .id("q123")
                    .content("更新后的题目内容")
                    .questionType(QuestionType.MULTIPLE_CHOICE)
                    .tags("更新,多选")
                    .updatedAt("2023-12-01T11:00:00")
                    .build();

            when(questionService.updateQuestion("q123", questionRequestDTO)).thenReturn(updatedQuestion);

            // when
            Result<QuestionDTO> response = questionController.updateQuestion("q123", questionRequestDTO);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(updatedQuestion);

            verify(questionService).updateQuestion("q123", questionRequestDTO);
        }

        @Test
        @DisplayName("更新不存在的题目")
        void shouldHandleUpdateNonexistentQuestion() {
            // given
            when(questionService.updateQuestion("nonexistent", questionRequestDTO))
                    .thenThrow(new RuntimeException("题目未找到"));

            // when & then
            try {
                questionController.updateQuestion("nonexistent", questionRequestDTO);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("题目未找到");
            }

            verify(questionService).updateQuestion("nonexistent", questionRequestDTO);
        }

        @Test
        @DisplayName("更新题目时参数无效")
        void shouldHandleInvalidUpdateParameters() {
            // given
            QuestionRequestDTO invalidRequest = new QuestionRequestDTO();
            when(questionService.updateQuestion("q123", invalidRequest))
                    .thenThrow(new RuntimeException("更新参数无效"));

            // when & then
            try {
                questionController.updateQuestion("q123", invalidRequest);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("更新参数无效");
            }

            verify(questionService).updateQuestion("q123", invalidRequest);
        }
    }

    @Nested
    @DisplayName("获取题目测试")
    class GetQuestionTests {

        @Test
        @DisplayName("根据ID获取题目成功")
        void shouldGetQuestionByIdSuccessfully() {
            // given
            when(questionService.getQuestionById("q123")).thenReturn(sampleQuestionDTO);

            // when
            Result<QuestionDTO> response = questionController.getQuestion("q123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(sampleQuestionDTO);

            verify(questionService).getQuestionById("q123");
        }

        @Test
        @DisplayName("根据不存在的ID获取题目")
        void shouldHandleGetNonexistentQuestion() {
            // given
            when(questionService.getQuestionById("nonexistent"))
                    .thenThrow(new RuntimeException("题目未找到"));

            // when & then
            try {
                questionController.getQuestion("nonexistent");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("题目未找到");
            }

            verify(questionService).getQuestionById("nonexistent");
        }
    }

    @Nested
    @DisplayName("分页查询题目测试")
    class GetQuestionsTests {

        @Test
        @DisplayName("成功分页获取题目列表")
        void shouldGetQuestionsSuccessfully() {
            // given
            Page<QuestionDTO> page = new PageImpl<>(questionList);
            when(questionService.getQuestions(eq(QuestionType.SINGLE_CHOICE), eq("keyword"), eq("tag1,tag2"), any(PageRequest.class)))
                    .thenReturn(page);

            // when
            Result<PageResult<QuestionDTO>> response = questionController.getQuestions(
                    QuestionType.SINGLE_CHOICE, "keyword", "tag1,tag2", 1, 10);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getList()).hasSize(1);
            assertThat(response.getData().getTotal()).isEqualTo(1);
            assertThat(response.getData().getPageNum()).isEqualTo(1);
            assertThat(response.getData().getPageSize()).isEqualTo(10);

            verify(questionService).getQuestions(eq(QuestionType.SINGLE_CHOICE), eq("keyword"), eq("tag1,tag2"), any(PageRequest.class));
        }

        @Test
        @DisplayName("使用默认分页参数获取题目列表")
        void shouldUseDefaultPaginationParameters() {
            // given
            Page<QuestionDTO> page = new PageImpl<>(questionList);
            when(questionService.getQuestions(isNull(), isNull(), isNull(), any(PageRequest.class)))
                    .thenReturn(page);

            // when
            Result<PageResult<QuestionDTO>> response = questionController.getQuestions(null, null, null, 1, 10);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getData().getPageNum()).isEqualTo(1);
            assertThat(response.getData().getPageSize()).isEqualTo(10);

            verify(questionService).getQuestions(isNull(), isNull(), isNull(), any(PageRequest.class));
        }

        @Test
        @DisplayName("获取空的题目列表")
        void shouldHandleEmptyQuestionList() {
            // given
            Page<QuestionDTO> emptyPage = new PageImpl<>(Arrays.asList());
            when(questionService.getQuestions(isNull(), isNull(), isNull(), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            // when
            Result<PageResult<QuestionDTO>> response = questionController.getQuestions(null, null, null, 1, 10);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getData().getList()).isEmpty();
            assertThat(response.getData().getTotal()).isEqualTo(0);

            verify(questionService).getQuestions(isNull(), isNull(), isNull(), any(PageRequest.class));
        }

        @Test
        @DisplayName("分页查询时服务异常")
        void shouldHandleServiceExceptionWhenPaging() {
            // given
            when(questionService.getQuestions(isNull(), isNull(), isNull(), any(PageRequest.class)))
                    .thenThrow(new RuntimeException("查询题目失败"));

            // when & then
            try {
                questionController.getQuestions(null, null, null, 1, 10);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("查询题目失败");
            }

            verify(questionService).getQuestions(isNull(), isNull(), isNull(), any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("搜索题目测试")
    class SearchQuestionsTests {

        @Test
        @DisplayName("成功搜索题目")
        void shouldSearchQuestionsSuccessfully() {
            // given
            when(questionService.searchQuestions("keyword")).thenReturn(questionList);

            // when
            Result<List<QuestionDTO>> response = questionController.searchQuestions("keyword");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).hasSize(1);
            assertThat(response.getData()).isEqualTo(questionList);

            verify(questionService).searchQuestions("keyword");
        }

        @Test
        @DisplayName("搜索题目返回空结果")
        void shouldHandleEmptySearchResults() {
            // given
            when(questionService.searchQuestions("nonexistent")).thenReturn(Arrays.asList());

            // when
            Result<List<QuestionDTO>> response = questionController.searchQuestions("nonexistent");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).isEmpty();

            verify(questionService).searchQuestions("nonexistent");
        }

        @Test
        @DisplayName("搜索题目时使用空关键词")
        void shouldHandleEmptySearchKeyword() {
            // given
            when(questionService.searchQuestions("")).thenReturn(Arrays.asList());

            // when
            Result<List<QuestionDTO>> response = questionController.searchQuestions("");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getData()).isEmpty();

            verify(questionService).searchQuestions("");
        }

        @Test
        @DisplayName("搜索题目时服务异常")
        void shouldHandleServiceExceptionWhenSearching() {
            // given
            when(questionService.searchQuestions("keyword"))
                    .thenThrow(new RuntimeException("搜索题目失败"));

            // when & then
            try {
                questionController.searchQuestions("keyword");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("搜索题目失败");
            }

            verify(questionService).searchQuestions("keyword");
        }
    }
}
