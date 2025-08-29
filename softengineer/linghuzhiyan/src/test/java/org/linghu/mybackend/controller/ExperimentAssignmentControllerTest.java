package org.linghu.mybackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.dto.UserDTO;
import org.linghu.mybackend.service.ExperimentAssignmentService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExperimentAssignmentController 单元测试 - 使用纯单元测试方式
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("实验分配控制器测试")
class ExperimentAssignmentControllerTest {

    @Mock
    private ExperimentAssignmentService experimentAssignmentService;

    @InjectMocks
    private ExperimentAssignmentController experimentAssignmentController;

    private List<UserDTO> userList;
    private Map<String, Object> batchAssignmentRequest;
    private Map<String, Object> batchRemovalRequest;

    @BeforeEach
    void setUp() {
        // 创建测试用户列表
        UserDTO user1 = new UserDTO();
        user1.setId("user1");
        user1.setUsername("student1");
        user1.setEmail("student1@test.com");

        UserDTO user2 = new UserDTO();
        user2.setId("user2");
        user2.setUsername("student2");
        user2.setEmail("student2@test.com");

        userList = Arrays.asList(user1, user2);

        // 创建批量分配请求
        batchAssignmentRequest = new HashMap<>();
        batchAssignmentRequest.put("userIds", Arrays.asList("user1", "user2"));

        // 创建批量移除请求
        batchRemovalRequest = new HashMap<>();
        batchRemovalRequest.put("userIds", Arrays.asList("user1", "user2"));
    }

    @Nested
    @DisplayName("批量分配实验任务测试")
    class BatchAssignTaskTests {

        @Test
        @DisplayName("成功批量分配任务给多个用户")
        void shouldBatchAssignTaskSuccessfully() {
            // given
            List<String> userIds = Arrays.asList("user1", "user2");
            doNothing().when(experimentAssignmentService).batchAssignTask("task123", userIds);

            // when
            Result<Void> response = experimentAssignmentController.assignTask("task123", batchAssignmentRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNull();

            verify(experimentAssignmentService).batchAssignTask("task123", userIds);
        }

        @Test
        @DisplayName("批量分配任务时用户列表为空")
        void shouldHandleEmptyUserListInBatchAssign() {
            // given
            Map<String, Object> emptyRequest = new HashMap<>();
            emptyRequest.put("userIds", Arrays.asList());

            // when
            Result<Void> response = experimentAssignmentController.assignTask("task123", emptyRequest);

            // then
            assertThat(response).isNotNull();
            // 根据实际控制器行为进行断言
            verify(experimentAssignmentService).batchAssignTask("task123", Arrays.asList());
        }

        @Test
        @DisplayName("批量分配任务时服务异常")
        void shouldHandleServiceExceptionInBatchAssign() {
            // given
            List<String> userIds = Arrays.asList("user1", "user2");
            doThrow(new RuntimeException("批量分配失败")).when(experimentAssignmentService).batchAssignTask("task123", userIds);

            // when & then
            try {
                experimentAssignmentController.assignTask("task123", batchAssignmentRequest);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("批量分配失败");
            }

            verify(experimentAssignmentService).batchAssignTask("task123", userIds);
        }

        @Test
        @DisplayName("批量分配任务给不存在的任务")
        void shouldHandleBatchAssignToNonexistentTask() {
            // given
            List<String> userIds = Arrays.asList("user1", "user2");
            doThrow(new RuntimeException("任务未找到")).when(experimentAssignmentService).batchAssignTask("nonexistent", userIds);

            // when & then
            try {
                experimentAssignmentController.assignTask("nonexistent", batchAssignmentRequest);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("任务未找到");
            }

            verify(experimentAssignmentService).batchAssignTask("nonexistent", userIds);
        }

        @Test
        @DisplayName("成功分配任务给全部学生")
        void shouldAssignTaskToAllStudentsSuccessfully() {
            // given
            doNothing().when(experimentAssignmentService).assignTaskToAllStudents("task123");

            // when
            Result<Void> response = experimentAssignmentController.assignTaskToAllStudents("task123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNull();

            verify(experimentAssignmentService).assignTaskToAllStudents("task123");
        }

        @Test
        @DisplayName("分配任务给全部学生时服务异常")
        void shouldHandleServiceExceptionWhenAssigningToAllStudents() {
            // given
            doThrow(new RuntimeException("分配给全部学生失败")).when(experimentAssignmentService).assignTaskToAllStudents("task123");

            // when & then
            try {
                experimentAssignmentController.assignTaskToAllStudents("task123");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("分配给全部学生失败");
            }

            verify(experimentAssignmentService).assignTaskToAllStudents("task123");
        }
    }

    @Nested
    @DisplayName("获取任务分配测试")
    class GetTaskAssignmentTests {

        @Test
        @DisplayName("成功获取任务分配列表")
        void shouldGetTaskAssignmentsSuccessfully() {
            // given
            when(experimentAssignmentService.getTaskAssignments("task123")).thenReturn(userList);

            // when
            Result<List<UserDTO>> response = experimentAssignmentController.getTaskAssignments("task123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).hasSize(2);
            assertThat(response.getData()).isEqualTo(userList);

            verify(experimentAssignmentService).getTaskAssignments("task123");
        }

        @Test
        @DisplayName("获取不存在任务的分配列表")
        void shouldHandleGetAssignmentsForNonexistentTask() {
            // given
            when(experimentAssignmentService.getTaskAssignments("nonexistent"))
                    .thenThrow(new RuntimeException("任务未找到"));

            // when & then
            try {
                experimentAssignmentController.getTaskAssignments("nonexistent");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("任务未找到");
            }

            verify(experimentAssignmentService).getTaskAssignments("nonexistent");
        }

        @Test
        @DisplayName("获取空的任务分配列表")
        void shouldHandleEmptyTaskAssignments() {
            // given
            when(experimentAssignmentService.getTaskAssignments("task123")).thenReturn(Arrays.asList());

            // when
            Result<List<UserDTO>> response = experimentAssignmentController.getTaskAssignments("task123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData()).isEmpty();

            verify(experimentAssignmentService).getTaskAssignments("task123");
        }
    }

    @Nested
    @DisplayName("批量取消任务分配测试")
    class BatchRemoveTaskAssignmentTests {

        @Test
        @DisplayName("成功批量取消任务分配")
        void shouldBatchRemoveTaskAssignmentSuccessfully() {
            // given
            List<String> userIds = Arrays.asList("user1", "user2");
            doNothing().when(experimentAssignmentService).batchRemoveTaskAssignment("task123", userIds);

            // when
            Result<Void> response = experimentAssignmentController.removeTaskAssignment(batchRemovalRequest, "task123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNull();

            verify(experimentAssignmentService).batchRemoveTaskAssignment("task123", userIds);
        }

        @Test
        @DisplayName("批量取消任务分配时用户列表为空")
        void shouldHandleEmptyUserListInBatchRemove() {
            // given
            Map<String, Object> emptyRequest = new HashMap<>();
            emptyRequest.put("userIds", Arrays.asList());

            // when
            Result<Void> response = experimentAssignmentController.removeTaskAssignment(emptyRequest, "task123");

            // then
            assertThat(response).isNotNull();
            // 根据实际控制器行为进行断言
            verify(experimentAssignmentService).batchRemoveTaskAssignment("task123", Arrays.asList());
        }

        @Test
        @DisplayName("批量取消不存在的任务分配")
        void shouldHandleBatchRemoveNonexistentTaskAssignment() {
            // given
            List<String> userIds = Arrays.asList("user1", "user2");
            doThrow(new RuntimeException("分配记录未找到")).when(experimentAssignmentService).batchRemoveTaskAssignment("nonexistent", userIds);

            // when & then
            try {
                experimentAssignmentController.removeTaskAssignment(batchRemovalRequest, "nonexistent");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("分配记录未找到");
            }

            verify(experimentAssignmentService).batchRemoveTaskAssignment("nonexistent", userIds);
        }

        @Test
        @DisplayName("批量取消任务分配时服务异常")
        void shouldHandleServiceExceptionWhenBatchRemoving() {
            // given
            List<String> userIds = Arrays.asList("user1", "user2");
            doThrow(new RuntimeException("批量取消分配失败")).when(experimentAssignmentService).batchRemoveTaskAssignment("task123", userIds);

            // when & then
            try {
                experimentAssignmentController.removeTaskAssignment(batchRemovalRequest, "task123");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("批量取消分配失败");
            }

            verify(experimentAssignmentService).batchRemoveTaskAssignment("task123", userIds);
        }

        @Test
        @DisplayName("使用无效参数批量取消任务分配")
        void shouldHandleInvalidParametersForBatchRemoval() {
            // given
            Map<String, Object> invalidRequest = new HashMap<>();
            // 不包含userIds字段，模拟无效请求

            // when
            Result<Void> response = experimentAssignmentController.removeTaskAssignment(invalidRequest, "task123");

            // then
            // 根据控制器实际行为，可能会传递null或空列表给service
            verify(experimentAssignmentService).batchRemoveTaskAssignment(eq("task123"), any());
        }
    }
}
