package org.linghu.mybackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.domain.Message;
import org.linghu.mybackend.repository.MessageRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 通知服务测试 - 专注于通知发送功能 - 完整迁移自微服务架构
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("通知服务测试")
class NotificationServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private Message sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Message.builder()
                .id("notif123")
                .title("测试通知")
                .content("测试通知内容")
                .sender("SYSTEM")
                .receiver("user123")
                .senderRole("SYSTEM")
                .status("未读")
                .build();
    }

    @Nested
    @DisplayName("系统通知发送测试")
    class SystemNotificationTests {

        @Test
        @DisplayName("成功发送系统通知给单个用户")
        void shouldSendSystemNotificationToSingleUser() {
            // given
            String title = "系统维护通知";
            String content = "系统将于今晚进行维护";
            List<String> receiverIds = Arrays.asList("user123");

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendSystemNotification(title, content, receiverIds);

            // then
            verify(messageRepository, times(1)).save(argThat(msg ->
                    msg.getTitle().equals(title) &&
                    msg.getContent().equals(content) &&
                    msg.getSender().equals("SYSTEM") &&
                    msg.getReceiver().equals("user123") &&
                    msg.getSenderRole().equals("SYSTEM") &&
                    msg.getStatus().equals("未读")
            ));
        }

        @Test
        @DisplayName("成功发送系统通知给多个用户")
        void shouldSendSystemNotificationToMultipleUsers() {
            // given
            String title = "重要通知";
            String content = "请及时查看新的课程安排";
            List<String> receiverIds = Arrays.asList("user1", "user2", "user3");

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendSystemNotification(title, content, receiverIds);

            // then
            verify(messageRepository, times(3)).save(argThat(msg ->
                    msg.getTitle().equals(title) &&
                    msg.getContent().equals(content) &&
                    msg.getSender().equals("SYSTEM") &&
                    msg.getSenderRole().equals("SYSTEM")
            ));
        }

        @Test
        @DisplayName("发送系统通知时单个用户保存失败不影响其他用户")
        void shouldContinueWhenSingleUserSaveFails() {
            // given
            String title = "通知标题";
            String content = "通知内容";
            List<String> receiverIds = Arrays.asList("user1", "user2", "user3");

            when(messageRepository.save(any(Message.class)))
                    .thenReturn(sampleNotification)  // user1 成功
                    .thenThrow(new RuntimeException("数据库错误"))  // user2 失败
                    .thenReturn(sampleNotification); // user3 成功

            // when
            assertThatNoException().isThrownBy(() ->
                    messageService.sendSystemNotification(title, content, receiverIds));

            // then
            verify(messageRepository, times(3)).save(any(Message.class));
        }

        @Test
        @DisplayName("发送系统通知给空用户列表")
        void shouldHandleEmptyReceiverList() {
            // given
            String title = "通知标题";
            String content = "通知内容";
            List<String> receiverIds = Arrays.asList();

            // when
            messageService.sendSystemNotification(title, content, receiverIds);

            // then
            verify(messageRepository, never()).save(any(Message.class));
        }
    }

    @Nested
    @DisplayName("实验通知发送测试")
    class ExperimentNotificationTests {

        @Test
        @DisplayName("成功发送实验通知")
        void shouldSendExperimentNotificationSuccessfully() {
            // given
            String title = "实验发布通知";
            String content = "新的算法实验已发布，请及时完成";
            String experimentId = "exp123";
            List<String> receiverIds = Arrays.asList("student1", "student2");

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendExperimentNotification(title, content, experimentId, receiverIds);

            // then
            verify(messageRepository, times(2)).save(argThat(msg ->
                    msg.getTitle().equals(title) &&
                    msg.getContent().equals(content) &&
                    msg.getSender().equals("SYSTEM") &&
                    msg.getSenderRole().equals("SYSTEM") &&
                    msg.getStatus().equals("未读")
            ));
        }

        @Test
        @DisplayName("发送实验通知时验证系统发送者")
        void shouldSendExperimentNotificationWithSystemSender() {
            // given
            String title = "紧急实验通知";
            String content = "实验截止时间提前";
            String experimentId = "exp456";
            List<String> receiverIds = Arrays.asList("student1");

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendExperimentNotification(title, content, experimentId, receiverIds);

            // then
            verify(messageRepository).save(argThat(msg ->
                    msg.getSender().equals("SYSTEM") &&
                    msg.getSenderRole().equals("SYSTEM")
            ));
        }

        @Test
        @DisplayName("发送实验通知时保存异常不影响其他学生")
        void shouldContinueWhenExperimentNotificationSaveFails() {
            // given
            String title = "实验通知";
            String content = "实验内容";
            String experimentId = "exp789";
            List<String> receiverIds = Arrays.asList("student1", "student2", "student3");

            when(messageRepository.save(any(Message.class)))
                    .thenReturn(sampleNotification)  // student1 成功
                    .thenThrow(new RuntimeException("保存失败"))  // student2 失败
                    .thenReturn(sampleNotification); // student3 成功

            // when
            assertThatNoException().isThrownBy(() ->
                    messageService.sendExperimentNotification(title, content, experimentId, receiverIds));

            // then
            verify(messageRepository, times(3)).save(any(Message.class));
        }
    }

    @Nested
    @DisplayName("成绩通知发送测试")
    class GradeNotificationTests {

        @Test
        @DisplayName("成功发送成绩通知")
        void shouldSendGradeNotificationSuccessfully() {
            // given
            String title = "实验成绩通知";
            String content = "您的算法实验成绩为85分";
            String experimentId = "exp123";
            String receiverId = "student1";

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendGradeNotification(title, content, experimentId, receiverId);

            // then
            verify(messageRepository).save(argThat(msg ->
                    msg.getTitle().equals(title) &&
                    msg.getContent().equals(content) &&
                    msg.getSender().equals("SYSTEM") &&
                    msg.getReceiver().equals(receiverId) &&
                    msg.getSenderRole().equals("SYSTEM") &&
                    msg.getStatus().equals("未读")
            ));
        }

        @Test
        @DisplayName("发送成绩通知时验证系统发送者")
        void shouldSendGradeNotificationWithSystemSender() {
            // given
            String title = "期末成绩通知";
            String content = "期末考试成绩已公布";
            String experimentId = "final_exam";
            String receiverId = "student123";

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendGradeNotification(title, content, experimentId, receiverId);

            // then
            verify(messageRepository).save(argThat(msg ->
                    msg.getSender().equals("SYSTEM") &&
                    msg.getSenderRole().equals("SYSTEM")
            ));
        }

        @Test
        @DisplayName("发送成绩通知失败时抛出异常")
        void shouldThrowExceptionWhenGradeNotificationSaveFails() {
            // given
            String title = "成绩通知";
            String content = "成绩内容";
            String experimentId = "exp456";
            String receiverId = "student1";

            when(messageRepository.save(any(Message.class)))
                    .thenThrow(new RuntimeException("数据库连接失败"));

            // when & then
            assertThatThrownBy(() ->
                    messageService.sendGradeNotification(title, content, experimentId, receiverId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("发送成绩通知失败");

            verify(messageRepository).save(any(Message.class));
        }

        @Test
        @DisplayName("发送成绩通知时验证接收者正确设置")
        void shouldSetReceiverCorrectlyInGradeNotification() {
            // given
            String title = "实验成绩";
            String content = "成绩详情";
            String experimentId = "advanced_algorithm_exp";
            String receiverId = "student999";

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendGradeNotification(title, content, experimentId, receiverId);

            // then
            verify(messageRepository).save(argThat(msg ->
                    msg.getReceiver().equals(receiverId)
            ));
        }
    }

    @Nested
    @DisplayName("通知内容验证测试")
    class NotificationContentTests {

        @Test
        @DisplayName("验证系统通知的发送者为SYSTEM")
        void shouldSetSystemAsSenderForAllNotifications() {
            // given
            String title = "测试通知";
            String content = "测试内容";
            List<String> receiverIds = Arrays.asList("user1");

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendSystemNotification(title, content, receiverIds);

            // then
            verify(messageRepository).save(argThat(msg ->
                    "SYSTEM".equals(msg.getSender()) &&
                    "SYSTEM".equals(msg.getSenderRole())
            ));
        }

        @Test
        @DisplayName("验证实验通知发送者为SYSTEM")
        void shouldSetSystemAsSenderForExperimentNotification() {
            // given
            String title = "实验通知";
            String content = "实验内容";
            String experimentId = "test_exp_123";
            List<String> receiverIds = Arrays.asList("student1");

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendExperimentNotification(title, content, experimentId, receiverIds);

            // then
            verify(messageRepository).save(argThat(msg ->
                    "SYSTEM".equals(msg.getSender()) &&
                    "SYSTEM".equals(msg.getSenderRole())
            ));
        }

        @Test
        @DisplayName("验证成绩通知只发送给指定接收者")
        void shouldSendGradeNotificationToSpecificReceiver() {
            // given
            String title = "成绩通知";
            String content = "您的成绩是90分";
            String experimentId = "exp123";
            String specificReceiver = "target_student";

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendGradeNotification(title, content, experimentId, specificReceiver);

            // then
            verify(messageRepository).save(argThat(msg ->
                    specificReceiver.equals(msg.getReceiver())
            ));
        }

        @Test
        @DisplayName("验证通知消息默认状态为未读")
        void shouldSetNotificationsAsUnreadByDefault() {
            // given
            String title = "状态测试通知";
            String content = "测试未读状态";
            List<String> receiverIds = Arrays.asList("user1");

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendSystemNotification(title, content, receiverIds);

            // then
            verify(messageRepository).save(argThat(msg ->
                    "未读".equals(msg.getStatus())
            ));
        }

        @Test
        @DisplayName("验证通知消息包含创建时间")
        void shouldSetCreatedTimeForNotifications() {
            // given
            String title = "时间测试通知";
            String content = "测试创建时间";
            List<String> receiverIds = Arrays.asList("user1");

            when(messageRepository.save(any(Message.class))).thenReturn(sampleNotification);

            // when
            messageService.sendSystemNotification(title, content, receiverIds);

            // then
            verify(messageRepository).save(argThat(msg ->
                    msg.getCreatedAt() != null && msg.getUpdatedAt() != null
            ));
        }
    }
}
