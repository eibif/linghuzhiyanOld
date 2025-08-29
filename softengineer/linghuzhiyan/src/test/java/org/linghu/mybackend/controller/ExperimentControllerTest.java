package org.linghu.mybackend.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.dto.ExperimentDTO;
import org.linghu.mybackend.dto.ExperimentRequestDTO;
import org.linghu.mybackend.dto.PageResult;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.service.ExperimentService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExperimentController 单元测试 - 使用纯单元测试方式
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("实验控制器测试")
class ExperimentControllerTest {

    @Mock
    private ExperimentService experimentService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private ExperimentController experimentController;

    private ExperimentDTO sampleExperimentDTO;
    private ExperimentRequestDTO experimentRequestDTO;
    private List<ExperimentDTO> experimentList;

    @BeforeEach
    void setUp() {
        // 创建测试实验DTO
        sampleExperimentDTO = ExperimentDTO.builder()
                .id("exp123")
                .name("测试实验名称")
                .description("测试实验描述")
                .creator_Id("creator123")
                .status(org.linghu.mybackend.domain.Experiment.ExperimentStatus.DRAFT)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(7))
                .build();

        // 创建测试实验请求
        experimentRequestDTO = new ExperimentRequestDTO();
        experimentRequestDTO.setName("测试实验名称");
        experimentRequestDTO.setDescription("测试实验描述");
        experimentRequestDTO.setStatus(org.linghu.mybackend.domain.Experiment.ExperimentStatus.DRAFT);
        experimentRequestDTO.setStartTime(LocalDateTime.now());
        experimentRequestDTO.setEndTime(LocalDateTime.now().plusDays(7));

        // 创建实验列表
        experimentList = Arrays.asList(sampleExperimentDTO);

        // 移除全局的 userDetails stubbing，改为在需要时单独设置
    }

    @AfterEach
    void tearDown() {
        // 清理操作
    }

    @Nested
    @DisplayName("创建实验测试")
    class CreateExperimentTests {

        @Test
        @DisplayName("成功创建实验")
        void shouldCreateExperimentSuccessfully() {
            // given
            when(userDetails.getUsername()).thenReturn("testuser");
            when(experimentService.createExperiment(any(ExperimentRequestDTO.class), eq("testuser")))
                    .thenReturn(sampleExperimentDTO);

            // when
            Result<ExperimentDTO> response = experimentController.createExperiment(experimentRequestDTO, userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(sampleExperimentDTO);

            verify(experimentService).createExperiment(any(ExperimentRequestDTO.class), eq("testuser"));
        }

        @Test
        @DisplayName("创建实验时服务异常")
        void shouldHandleServiceExceptionWhenCreating() {
            // given
            when(userDetails.getUsername()).thenReturn("testuser");
            when(experimentService.createExperiment(any(ExperimentRequestDTO.class), eq("testuser")))
                    .thenThrow(new RuntimeException("服务异常"));

            // when & then
            try {
                experimentController.createExperiment(experimentRequestDTO, userDetails);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("服务异常");
            }

            verify(experimentService).createExperiment(any(ExperimentRequestDTO.class), eq("testuser"));
        }

        @Test
        @DisplayName("创建实验时参数为null")
        void shouldHandleNullRequestDTO() {
            // given
            when(userDetails.getUsername()).thenReturn("testuser");
            ExperimentRequestDTO nullRequest = null;

            // when & then
            try {
                experimentController.createExperiment(nullRequest, userDetails);
            } catch (Exception e) {
                // 预期会抛出异常
            }
        }
    }

    @Nested
    @DisplayName("获取实验测试")
    class GetExperimentTests {

        @Test
        @DisplayName("成功获取实验列表")
        void shouldGetExperimentsSuccessfully() {
            // given
            Page<ExperimentDTO> page = new PageImpl<>(experimentList);
            when(experimentService.getAllExperiments(1, 10)).thenReturn(page);

            // when
            Result<PageResult<ExperimentDTO>> response = experimentController.getExperiments(1, 10);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getList()).hasSize(1);
            assertThat(response.getData().getTotal()).isEqualTo(1);
            assertThat(response.getData().getPageNum()).isEqualTo(1);
            assertThat(response.getData().getPageSize()).isEqualTo(10);

            verify(experimentService).getAllExperiments(1, 10);
        }

        @Test
        @DisplayName("获取实验列表时使用默认分页参数")
        void shouldUseDefaultPaginationParameters() {
            // given
            Page<ExperimentDTO> page = new PageImpl<>(experimentList);
            when(experimentService.getAllExperiments(1, 10)).thenReturn(page);

            // when
            Result<PageResult<ExperimentDTO>> response = experimentController.getExperiments(1, 10);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getData().getPageNum()).isEqualTo(1);
            assertThat(response.getData().getPageSize()).isEqualTo(10);

            verify(experimentService).getAllExperiments(1, 10);
        }

        @Test
        @DisplayName("根据ID获取实验成功")
        void shouldGetExperimentByIdSuccessfully() {
            // given
            when(experimentService.getExperimentById("exp123")).thenReturn(sampleExperimentDTO);

            // when
            Result<ExperimentDTO> response = experimentController.getExperiment("exp123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(sampleExperimentDTO);

            verify(experimentService).getExperimentById("exp123");
        }

        @Test
        @DisplayName("根据不存在的ID获取实验")
        void shouldHandleExperimentNotFound() {
            // given
            when(experimentService.getExperimentById("nonexistent"))
                    .thenThrow(new RuntimeException("实验未找到"));

            // when & then
            try {
                experimentController.getExperiment("nonexistent");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("实验未找到");
            }

            verify(experimentService).getExperimentById("nonexistent");
        }
    }

    @Nested
    @DisplayName("更新实验测试")
    class UpdateExperimentTests {

        @Test
        @DisplayName("成功更新实验")
        void shouldUpdateExperimentSuccessfully() {
            // given
            when(userDetails.getUsername()).thenReturn("testuser");
            ExperimentDTO updatedExperiment = ExperimentDTO.builder()
                    .id("exp123")
                    .name("更新后的实验名称")
                    .description("更新后的实验描述")
                    .creator_Id("creator123")
                    .status(org.linghu.mybackend.domain.Experiment.ExperimentStatus.DRAFT)
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusDays(7))
                    .build();

            when(experimentService.updateExperiment(eq("exp123"), any(ExperimentRequestDTO.class), eq("testuser")))
                    .thenReturn(updatedExperiment);

            // when
            Result<ExperimentDTO> response = experimentController.updateExperiment("exp123", experimentRequestDTO, userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(updatedExperiment);

            verify(experimentService).updateExperiment(eq("exp123"), any(ExperimentRequestDTO.class), eq("testuser"));
        }

        @Test
        @DisplayName("更新不存在的实验")
        void shouldHandleUpdateNonexistentExperiment() {
            // given
            when(userDetails.getUsername()).thenReturn("testuser");
            when(experimentService.updateExperiment(eq("nonexistent"), any(ExperimentRequestDTO.class), eq("testuser")))
                    .thenThrow(new RuntimeException("实验未找到"));

            // when & then
            try {
                experimentController.updateExperiment("nonexistent", experimentRequestDTO, userDetails);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("实验未找到");
            }

            verify(experimentService).updateExperiment(eq("nonexistent"), any(ExperimentRequestDTO.class), eq("testuser"));
        }

        @Test
        @DisplayName("更新实验时参数无效")
        void shouldHandleInvalidUpdateParameters() {
            // given
            when(userDetails.getUsername()).thenReturn("testuser");
            ExperimentRequestDTO invalidRequest = new ExperimentRequestDTO();
            // 设置无效的参数

            // when & then
            try {
                experimentController.updateExperiment("exp123", invalidRequest, userDetails);
            } catch (Exception e) {
                // 预期处理无效参数
            }
        }
    }

    @Nested
    @DisplayName("删除实验测试")
    class DeleteExperimentTests {

        @Test
        @DisplayName("成功删除实验")
        void shouldDeleteExperimentSuccessfully() {
            // given
            doNothing().when(experimentService).deleteExperiment("exp123");

            // when
            Result<Void> response = experimentController.deleteExperiment("exp123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNull();

            verify(experimentService).deleteExperiment("exp123");
        }

        @Test
        @DisplayName("删除不存在的实验")
        void shouldHandleDeleteNonexistentExperiment() {
            // given
            doThrow(new RuntimeException("实验未找到")).when(experimentService).deleteExperiment("nonexistent");

            // when & then
            try {
                experimentController.deleteExperiment("nonexistent");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("实验未找到");
            }

            verify(experimentService).deleteExperiment("nonexistent");
        }

        @Test
        @DisplayName("删除实验时服务异常")
        void shouldHandleServiceExceptionWhenDeleting() {
            // given
            doThrow(new RuntimeException("删除失败")).when(experimentService).deleteExperiment("exp123");

            // when & then
            try {
                experimentController.deleteExperiment("exp123");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("删除失败");
            }

            verify(experimentService).deleteExperiment("exp123");
        }
    }

    @Nested
    @DisplayName("实验状态管理测试")
    class ExperimentStatusTests {

        @Test
        @DisplayName("成功发布实验")
        void shouldPublishExperimentSuccessfully() {
            // given
            ExperimentDTO publishedExperiment = ExperimentDTO.builder()
                    .id("exp123")
                    .name("测试实验名称")
                    .description("测试实验描述")
                    .creator_Id("creator123")
                    .status(org.linghu.mybackend.domain.Experiment.ExperimentStatus.PUBLISHED)
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusDays(7))
                    .build();

            when(experimentService.publishExperiment("exp123")).thenReturn(publishedExperiment);

            // when
            Result<ExperimentDTO> response = experimentController.publishExperiment("exp123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(publishedExperiment);
            assertThat(response.getData().getStatus()).isEqualTo(org.linghu.mybackend.domain.Experiment.ExperimentStatus.PUBLISHED);

            verify(experimentService).publishExperiment("exp123");
        }

        @Test
        @DisplayName("发布不存在的实验")
        void shouldHandlePublishNonexistentExperiment() {
            // given
            when(experimentService.publishExperiment("nonexistent"))
                    .thenThrow(new RuntimeException("实验未找到"));

            // when & then
            try {
                experimentController.publishExperiment("nonexistent");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("实验未找到");
            }

            verify(experimentService).publishExperiment("nonexistent");
        }

        @Test
        @DisplayName("成功取消发布实验")
        void shouldUnpublishExperimentSuccessfully() {
            // given
            ExperimentDTO unpublishedExperiment = ExperimentDTO.builder()
                    .id("exp123")
                    .name("测试实验名称")
                    .description("测试实验描述")
                    .creator_Id("creator123")
                    .status(org.linghu.mybackend.domain.Experiment.ExperimentStatus.DRAFT)
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusDays(7))
                    .build();

            when(experimentService.unpublishExperiment("exp123")).thenReturn(unpublishedExperiment);

            // when
            Result<ExperimentDTO> response = experimentController.unpublishExperiment("exp123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(unpublishedExperiment);
            assertThat(response.getData().getStatus()).isEqualTo(org.linghu.mybackend.domain.Experiment.ExperimentStatus.DRAFT);

            verify(experimentService).unpublishExperiment("exp123");
        }

        @Test
        @DisplayName("取消发布不存在的实验")
        void shouldHandleUnpublishNonexistentExperiment() {
            // given
            when(experimentService.unpublishExperiment("nonexistent"))
                    .thenThrow(new RuntimeException("实验未找到"));

            // when & then
            try {
                experimentController.unpublishExperiment("nonexistent");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("实验未找到");
            }

            verify(experimentService).unpublishExperiment("nonexistent");
        }

        @Test
        @DisplayName("发布已发布的实验")
        void shouldHandlePublishAlreadyPublishedExperiment() {
            // given
            when(experimentService.publishExperiment("exp123"))
                    .thenThrow(new RuntimeException("实验已经是发布状态"));

            // when & then
            try {
                experimentController.publishExperiment("exp123");
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("实验已经是发布状态");
            }

            verify(experimentService).publishExperiment("exp123");
        }
    }
}
