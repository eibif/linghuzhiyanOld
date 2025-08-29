package org.linghu.mybackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.dto.*;
import org.linghu.mybackend.constants.TaskType;
import org.linghu.mybackend.domain.Experiment;
import org.linghu.mybackend.service.StudentExperimentService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * StudentExperimentController 单元测试 - 使用纯单元测试方式
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("学生实验参与控制器测试")
class StudentExperimentControllerTest {

    @Mock
    private StudentExperimentService studentExperimentService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private StudentExperimentController studentExperimentController;

    private ExperimentDTO sampleExperimentDTO;
    private ExperimentTaskDTO sampleTaskDTO;
    private ExperimentSubmissionDTO sampleSubmissionDTO;
    private ExperimentEvaluationDTO sampleEvaluationDTO;
    private SubmissionRequestDTO submissionRequestDTO;
    private List<ExperimentDTO> experimentList;
    private List<ExperimentTaskDTO> taskList;
    private List<ExperimentEvaluationDTO> evaluationList;

    @BeforeEach
    void setUp() {
        // 移除全局的 userDetails stubbing，改为在需要时单独设置

        // 创建测试实验DTO
        sampleExperimentDTO = ExperimentDTO.builder()
                .id("exp123")
                .name("测试实验名称")
                .description("测试实验描述")
                .creator_Id("teacher123")
                .status(Experiment.ExperimentStatus.PUBLISHED)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(7))
                .build();

        // 创建测试任务DTO
        sampleTaskDTO = ExperimentTaskDTO.builder()
                .id("task123")
                .title("测试任务标题")
                .description("测试任务描述")
                .experimentId("exp123")
                .orderNum(1)
                .taskType(TaskType.CODE)
                .required(true)
                .build();

        // 创建测试提交DTO
        sampleSubmissionDTO = ExperimentSubmissionDTO.builder()
                .id("sub123")
                .task_id("task123")
                .user_id("student123")
                .user_answer("测试提交内容")
                .submission_time(LocalDateTime.now())
                .build();

        // 创建测试评估DTO
        sampleEvaluationDTO = ExperimentEvaluationDTO.builder()
                .id("eval123")
                .submissionId("sub123")
                .taskId("task123")
                .userId("student123")
                .score(new BigDecimal("85"))
                .userAnswer("良好的解决方案")
                .submitTime(LocalDateTime.now())
                .build();

        // 创建提交请求DTO
        submissionRequestDTO = SubmissionRequestDTO.builder()
                .taskId("task123")
                .experimentId("exp123")
                .userAnswer("测试提交内容")
                .build();

        // 创建列表
        experimentList = Arrays.asList(sampleExperimentDTO);
        taskList = Arrays.asList(sampleTaskDTO);
        evaluationList = Arrays.asList(sampleEvaluationDTO);
    }

    @Nested
    @DisplayName("获取实验列表测试")
    class GetExperimentsTests {

        @Test
        @DisplayName("成功获取学生实验列表")
        void shouldGetStudentExperimentsSuccessfully() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getStudentExperiments("student123")).thenReturn(experimentList);

            // when
            Result<List<ExperimentDTO>> response = studentExperimentController.getExperiments(userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).hasSize(1);
            assertThat(response.getData()).isEqualTo(experimentList);

            verify(studentExperimentService).getStudentExperiments("student123");
        }

        @Test
        @DisplayName("获取空的实验列表")
        void shouldHandleEmptyExperimentList() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getStudentExperiments("student123")).thenReturn(Arrays.asList());

            // when
            Result<List<ExperimentDTO>> response = studentExperimentController.getExperiments(userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).isEmpty();

            verify(studentExperimentService).getStudentExperiments("student123");
        }

        @Test
        @DisplayName("获取实验列表时服务异常")
        void shouldHandleServiceExceptionWhenGettingExperiments() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getStudentExperiments("student123"))
                    .thenThrow(new RuntimeException("获取实验列表失败"));

            // when & then
            try {
                studentExperimentController.getExperiments(userDetails);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("获取实验列表失败");
            }

            verify(studentExperimentService).getStudentExperiments("student123");
        }
    }

    @Nested
    @DisplayName("获取实验详情测试")
    class GetExperimentDetailsTests {

        @Test
        @DisplayName("成功获取实验详情")
        void shouldGetExperimentDetailsSuccessfully() {
            // given
            when(studentExperimentService.getExperimentDetails("exp123")).thenReturn(sampleExperimentDTO);

            // when
            Result<ExperimentDTO> response = studentExperimentController.getExperiment("exp123", userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(sampleExperimentDTO);

            verify(studentExperimentService).getExperimentDetails("exp123");
        }

        @Test
        @DisplayName("获取不存在实验的详情")
        void shouldHandleGetNonexistentExperimentDetails() {
            // given
            when(studentExperimentService.getExperimentDetails("nonexistent"))
                    .thenThrow(new RuntimeException("实验未找到"));

            // when & then
            try {
                studentExperimentController.getExperiment("nonexistent", userDetails);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("实验未找到");
            }

            verify(studentExperimentService).getExperimentDetails("nonexistent");
        }
    }

    @Nested
    @DisplayName("获取分配任务测试")
    class GetAssignedTasksTests {

        @Test
        @DisplayName("成功获取已分配任务")
        void shouldGetAssignedTasksSuccessfully() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getAssignedTasks("student123")).thenReturn(taskList);

            // when
            Result<List<ExperimentTaskDTO>> response = studentExperimentController.getAssignedTasks(userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).hasSize(1);
            assertThat(response.getData()).isEqualTo(taskList);

            verify(studentExperimentService).getAssignedTasks("student123");
        }

        @Test
        @DisplayName("获取空的任务列表")
        void shouldHandleEmptyAssignedTasksList() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getAssignedTasks("student123")).thenReturn(Arrays.asList());

            // when
            Result<List<ExperimentTaskDTO>> response = studentExperimentController.getAssignedTasks(userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).isEmpty();

            verify(studentExperimentService).getAssignedTasks("student123");
        }
    }

    @Nested
    @DisplayName("获取具体任务测试")
    class GetTaskTests {

        @Test
        @DisplayName("成功获取具体任务")
        void shouldGetTaskSuccessfully() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getTaskById("task123", "student123")).thenReturn(sampleTaskDTO);

            // when
            Result<ExperimentTaskDTO> response = studentExperimentController.getTask("task123", userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(sampleTaskDTO);

            verify(studentExperimentService).getTaskById("task123", "student123");
        }

        @Test
        @DisplayName("获取无权限访问的任务")
        void shouldHandleGetUnauthorizedTask() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getTaskById("task123", "student123"))
                    .thenThrow(new RuntimeException("无权限访问该任务"));

            // when & then
            try {
                studentExperimentController.getTask("task123", userDetails);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("无权限访问该任务");
            }

            verify(studentExperimentService).getTaskById("task123", "student123");
        }
    }

    @Nested
    @DisplayName("提交任务测试")
    class SubmitTaskTests {

        @Test
        @DisplayName("成功提交任务")
        void shouldSubmitTaskSuccessfully() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.submitTask(submissionRequestDTO, "student123")).thenReturn(sampleSubmissionDTO);

            // when
            Result<ExperimentSubmissionDTO> response = studentExperimentController.submitTask(submissionRequestDTO, userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(sampleSubmissionDTO);

            verify(studentExperimentService).submitTask(submissionRequestDTO, "student123");
        }

        @Test
        @DisplayName("提交任务时参数无效")
        void shouldHandleInvalidSubmissionRequest() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            SubmissionRequestDTO invalidRequest = new SubmissionRequestDTO();
            when(studentExperimentService.submitTask(invalidRequest, "student123"))
                    .thenThrow(new RuntimeException("提交参数无效"));

            // when & then
            try {
                studentExperimentController.submitTask(invalidRequest, userDetails);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("提交参数无效");
            }

            verify(studentExperimentService).submitTask(invalidRequest, "student123");
        }

        @Test
        @DisplayName("提交已截止任务")
        void shouldHandleSubmitExpiredTask() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.submitTask(submissionRequestDTO, "student123"))
                    .thenThrow(new RuntimeException("任务已截止"));

            // when & then
            try {
                studentExperimentController.submitTask(submissionRequestDTO, userDetails);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("任务已截止");
            }

            verify(studentExperimentService).submitTask(submissionRequestDTO, "student123");
        }
    }

    @Nested
    @DisplayName("获取评测结果测试")
    class GetEvaluationResultTests {

        @Test
        @DisplayName("成功获取评测结果")
        void shouldGetTaskEvaluationResultSuccessfully() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getTaskEvaluationResult("task123", "student123")).thenReturn(sampleEvaluationDTO);

            // when
            Result<ExperimentEvaluationDTO> response = studentExperimentController.getTaskEvaluationResult("task123", userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(sampleEvaluationDTO);

            verify(studentExperimentService).getTaskEvaluationResult("task123", "student123");
        }

        @Test
        @DisplayName("获取未评测任务的结果")
        void shouldHandleGetUnevaluatedTaskResult() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getTaskEvaluationResult("task123", "student123"))
                    .thenThrow(new RuntimeException("任务尚未评测"));

            // when & then
            try {
                studentExperimentController.getTaskEvaluationResult("task123", userDetails);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("任务尚未评测");
            }

            verify(studentExperimentService).getTaskEvaluationResult("task123", "student123");
        }
    }

    @Nested
    @DisplayName("获取评测历史测试")
    class GetEvaluationHistoryTests {

        @Test
        @DisplayName("成功获取评测历史")
        void shouldGetTaskEvaluationHistorySuccessfully() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getTaskEvaluationHistory("task123", "student123")).thenReturn(evaluationList);

            // when
            Result<List<ExperimentEvaluationDTO>> response = studentExperimentController.getTaskEvaluationHistory("task123", userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).hasSize(1);
            assertThat(response.getData()).isEqualTo(evaluationList);

            verify(studentExperimentService).getTaskEvaluationHistory("task123", "student123");
        }

        @Test
        @DisplayName("获取空的评测历史")
        void shouldHandleEmptyEvaluationHistory() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getTaskEvaluationHistory("task123", "student123")).thenReturn(Arrays.asList());

            // when
            Result<List<ExperimentEvaluationDTO>> response = studentExperimentController.getTaskEvaluationHistory("task123", userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).isEmpty();

            verify(studentExperimentService).getTaskEvaluationHistory("task123", "student123");
        }

        @Test
        @DisplayName("获取评测历史时服务异常")
        void shouldHandleServiceExceptionWhenGettingHistory() {
            // given
            when(userDetails.getUsername()).thenReturn("student123");
            when(studentExperimentService.getTaskEvaluationHistory("task123", "student123"))
                    .thenThrow(new RuntimeException("获取评测历史失败"));

            // when & then
            try {
                studentExperimentController.getTaskEvaluationHistory("task123", userDetails);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("获取评测历史失败");
            }

            verify(studentExperimentService).getTaskEvaluationHistory("task123", "student123");
        }
    }
}
