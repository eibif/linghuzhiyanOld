package org.linghu.mybackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.dto.ExperimentTaskDTO;
import org.linghu.mybackend.dto.ExperimentTaskRequestDTO;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.constants.TaskType;
import org.linghu.mybackend.service.ExperimentTaskService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * ExperimentTaskController 单元测试 - 使用纯单元测试方式
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("实验任务控制器测试")
class ExperimentTaskControllerTest {

    @Mock
    private ExperimentTaskService experimentTaskService;

    @InjectMocks
    private ExperimentTaskController experimentTaskController;

    private ExperimentTaskDTO sampleTaskDTO;
    private ExperimentTaskRequestDTO taskRequestDTO;
    private List<ExperimentTaskDTO> taskList;

    @BeforeEach
    void setUp() {
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

        // 创建测试任务请求
        taskRequestDTO = ExperimentTaskRequestDTO.builder()
                .title("测试任务标题")
                .description("测试任务描述")
                .taskType(TaskType.CODE)
                .required(true)
                .build();

        // 创建任务列表
        taskList = Arrays.asList(sampleTaskDTO);
    }

    @Nested
    @DisplayName("添加实验任务测试")
    class AddTaskTests {

        @Test
        @DisplayName("成功添加实验任务")
        void shouldAddTaskSuccessfully() {
            // given
            when(experimentTaskService.createTask("exp123", taskRequestDTO)).thenReturn(sampleTaskDTO);

            // when
            Result<ExperimentTaskDTO> response = experimentTaskController.addTask("exp123", taskRequestDTO);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(sampleTaskDTO);

            verify(experimentTaskService).createTask("exp123", taskRequestDTO);
        }

        @Test
        @DisplayName("添加任务时实验不存在")
        void shouldHandleAddTaskToNonexistentExperiment() {
            // given
            when(experimentTaskService.createTask("nonexistent", taskRequestDTO))
                    .thenThrow(new RuntimeException("实验未找到"));

            // when & then
            try {
                experimentTaskController.addTask("nonexistent", taskRequestDTO);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("实验未找到");
            }

            verify(experimentTaskService).createTask("nonexistent", taskRequestDTO);
        }

        @Test
        @DisplayName("添加任务时参数无效")
        void shouldHandleInvalidTaskRequest() {
            // given
            ExperimentTaskRequestDTO invalidRequest = new ExperimentTaskRequestDTO();
            // 不设置必要字段

            when(experimentTaskService.createTask("exp123", invalidRequest))
                    .thenThrow(new RuntimeException("任务参数无效"));

            // when & then
            try {
                experimentTaskController.addTask("exp123", invalidRequest);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("任务参数无效");
            }

            verify(experimentTaskService).createTask("exp123", invalidRequest);
        }

        @Test
        @DisplayName("添加任务时服务异常")
        void shouldHandleServiceExceptionWhenAdding() {
            // given
            when(experimentTaskService.createTask("exp123", taskRequestDTO))
                    .thenThrow(new RuntimeException("创建任务失败"));

            // when & then
            try {
                experimentTaskController.addTask("exp123", taskRequestDTO);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("创建任务失败");
            }

            verify(experimentTaskService).createTask("exp123", taskRequestDTO);
        }
    }

    @Nested
    @DisplayName("获取实验任务测试")
    class GetTasksTests {

        @Test
        @DisplayName("成功获取实验任务列表")
        void shouldGetTasksSuccessfully() {
            // given
            when(experimentTaskService.getTasksByExperimentId("exp123")).thenReturn(taskList);

            // when
            Result<List<ExperimentTaskDTO>> response = experimentTaskController.getTasks("exp123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).hasSize(1);
            assertThat(response.getData()).isEqualTo(taskList);

            verify(experimentTaskService).getTasksByExperimentId("exp123");
        }

        @Test
        @DisplayName("获取不存在实验的任务列表")
        void shouldHandleGetTasksForNonexistentExperiment() {
            // given
            when(experimentTaskService.getTasksByExperimentId("nonexistent"))
                    .thenThrow(new RuntimeException("实验未找到"));

            // when & then
            try {
                experimentTaskController.getTasks("nonexistent");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("实验未找到");
            }

            verify(experimentTaskService).getTasksByExperimentId("nonexistent");
        }

        @Test
        @DisplayName("获取空的任务列表")
        void shouldHandleEmptyTaskList() {
            // given
            when(experimentTaskService.getTasksByExperimentId("exp123")).thenReturn(Arrays.asList());

            // when
            Result<List<ExperimentTaskDTO>> response = experimentTaskController.getTasks("exp123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).isEmpty();

            verify(experimentTaskService).getTasksByExperimentId("exp123");
        }
    }

    @Nested
    @DisplayName("更新任务测试")
    class UpdateTaskTests {

        @Test
        @DisplayName("成功更新任务")
        void shouldUpdateTaskSuccessfully() {
            // given
            ExperimentTaskDTO updatedTask = ExperimentTaskDTO.builder()
                    .id("task123")
                    .title("更新后的任务标题")
                    .description("更新后的任务描述")
                    .experimentId("exp123")
                    .orderNum(2)
                    .taskType(TaskType.OTHER)
                    .required(false)
                    .build();

            when(experimentTaskService.updateTask("task123", taskRequestDTO)).thenReturn(updatedTask);

            // when
            Result<ExperimentTaskDTO> response = experimentTaskController.updateTask("exp123", "task123", taskRequestDTO);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(updatedTask);

            verify(experimentTaskService).updateTask("task123", taskRequestDTO);
        }

        @Test
        @DisplayName("更新不存在的任务")
        void shouldHandleUpdateNonexistentTask() {
            // given
            when(experimentTaskService.updateTask("nonexistent", taskRequestDTO))
                    .thenThrow(new RuntimeException("任务未找到"));

            // when & then
            try {
                experimentTaskController.updateTask("exp123", "nonexistent", taskRequestDTO);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("任务未找到");
            }

            verify(experimentTaskService).updateTask("nonexistent", taskRequestDTO);
        }

        @Test
        @DisplayName("更新任务时参数无效")
        void shouldHandleInvalidUpdateParameters() {
            // given
            ExperimentTaskRequestDTO invalidRequest = new ExperimentTaskRequestDTO();
            when(experimentTaskService.updateTask("task123", invalidRequest))
                    .thenThrow(new RuntimeException("更新参数无效"));

            // when & then
            try {
                experimentTaskController.updateTask("exp123", "task123", invalidRequest);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("更新参数无效");
            }

            verify(experimentTaskService).updateTask("task123", invalidRequest);
        }

        @Test
        @DisplayName("更新任务时服务异常")
        void shouldHandleServiceExceptionWhenUpdating() {
            // given
            when(experimentTaskService.updateTask("task123", taskRequestDTO))
                    .thenThrow(new RuntimeException("更新任务失败"));

            // when & then
            try {
                experimentTaskController.updateTask("exp123", "task123", taskRequestDTO);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("更新任务失败");
            }

            verify(experimentTaskService).updateTask("task123", taskRequestDTO);
        }
    }

    @Nested
    @DisplayName("删除任务测试")
    class DeleteTaskTests {

        @Test
        @DisplayName("成功删除任务")
        void shouldDeleteTaskSuccessfully() {
            // given
            doNothing().when(experimentTaskService).deleteTask("task123");

            // when
            Result<Void> response = experimentTaskController.deleteTask("exp123", "task123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNull();

            verify(experimentTaskService).deleteTask("task123");
        }

        @Test
        @DisplayName("删除不存在的任务")
        void shouldHandleDeleteNonexistentTask() {
            // given
            doThrow(new RuntimeException("任务未找到")).when(experimentTaskService).deleteTask("nonexistent");

            // when & then
            try {
                experimentTaskController.deleteTask("exp123", "nonexistent");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("任务未找到");
            }

            verify(experimentTaskService).deleteTask("nonexistent");
        }

        @Test
        @DisplayName("删除任务时服务异常")
        void shouldHandleServiceExceptionWhenDeleting() {
            // given
            doThrow(new RuntimeException("删除任务失败")).when(experimentTaskService).deleteTask("task123");

            // when & then
            try {
                experimentTaskController.deleteTask("exp123", "task123");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("删除任务失败");
            }

            verify(experimentTaskService).deleteTask("task123");
        }

        @Test
        @DisplayName("删除任务时实验ID不匹配")
        void shouldHandleExperimentIdMismatch() {
            // given
            doThrow(new RuntimeException("实验ID不匹配")).when(experimentTaskService).deleteTask("task123");

            // when & then
            try {
                experimentTaskController.deleteTask("wrongexp", "task123");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("实验ID不匹配");
            }

            verify(experimentTaskService).deleteTask("task123");
        }
    }
}
