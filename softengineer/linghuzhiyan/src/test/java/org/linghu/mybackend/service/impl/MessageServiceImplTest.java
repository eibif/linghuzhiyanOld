package org.linghu.mybackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.domain.Message;
import org.linghu.mybackend.domain.User;
import org.linghu.mybackend.dto.MessageDTO;
import org.linghu.mybackend.repository.MessageRepository;
import org.linghu.mybackend.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MessageService 单元测试 - 完整迁移自微服务架构
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("消息服务测试")
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private Message sampleMessage;
    private MessageDTO messageDTO;
    private User senderUser;
    private User receiverUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户信息
        senderUser = new User();
        senderUser.setId("sender123");
        senderUser.setUsername("testSender");

        receiverUser = new User();
        receiverUser.setId("receiver456");
        receiverUser.setUsername("testReceiver");

        // 创建测试消息
        sampleMessage = Message.builder()
                .id("msg123")
                .title("测试消息标题")
                .content("测试消息内容")
                .sender("testSender")
                .receiver("testReceiver")
                .senderRole("TEACHER")
                .status("未读")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 创建测试消息DTO
        messageDTO = MessageDTO.builder()
                .title("测试消息标题")
                .content("测试消息内容")
                .sender("testSender")
                .receiver("testReceiver")
                .senderRole("TEACHER")
                .build();
    }

    @Nested
    @DisplayName("创建消息测试")
    class CreateMessageTests {

        @Test
        @DisplayName("成功创建消息")
        void shouldCreateMessageSuccessfully() {
            // given
            when(messageRepository.save(any(Message.class))).thenReturn(sampleMessage);

            // when
            MessageDTO result = messageService.createMessage(messageDTO);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("测试消息标题");
            assertThat(result.getContent()).isEqualTo("测试消息内容");
            assertThat(result.getSender()).isEqualTo("testSender");
            assertThat(result.getReceiver()).isEqualTo("testReceiver");
            assertThat(result.getStatus()).isEqualTo("未读");

            verify(messageRepository).save(any(Message.class));
        }

        @Test
        @DisplayName("创建消息时应该设置正确的初始状态")
        void shouldSetCorrectInitialStatusWhenCreating() {
            // given
            when(messageRepository.save(any(Message.class))).thenReturn(sampleMessage);

            // when
            MessageDTO result = messageService.createMessage(messageDTO);

            // then
            assertThat(result.getStatus()).isEqualTo("未读");
            verify(messageRepository).save(argThat(msg -> 
                "未读".equals(msg.getStatus()) && msg.getCreatedAt() != null));
        }
    }

    @Nested
    @DisplayName("获取消息测试")
    class GetMessageTests {

        @Test
        @DisplayName("根据ID成功获取消息")
        void shouldGetMessageByIdSuccessfully() {
            // given
            when(messageRepository.findById("msg123")).thenReturn(Optional.of(sampleMessage));

            // when
            MessageDTO result = messageService.getMessageById("msg123");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("msg123");
            assertThat(result.getTitle()).isEqualTo("测试消息标题");

            verify(messageRepository).findById("msg123");
        }

        @Test
        @DisplayName("消息不存在时获取失败")
        void shouldReturnNullWhenMessageNotFound() {
            // given
            when(messageRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // when
            MessageDTO result = messageService.getMessageById("nonexistent");

            // then
            assertThat(result).isNull();

            verify(messageRepository).findById("nonexistent");
        }

        @Test
        @DisplayName("成功获取接收者消息列表")
        void shouldGetMessagesByReceiverSuccessfully() {
            // given
            List<Message> messages = Arrays.asList(sampleMessage);
            when(messageRepository.findByReceiverOrderByCreatedAtDesc("testReceiver"))
                    .thenReturn(messages);

            // when
            List<MessageDTO> result = messageService.getMessagesByReceiver("testReceiver");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReceiver()).isEqualTo("testReceiver");

            verify(messageRepository).findByReceiverOrderByCreatedAtDesc("testReceiver");
        }

        @Test
        @DisplayName("成功获取发送者和接收者之间的消息")
        void shouldGetMessagesBySenderAndReceiverSuccessfully() {
            // given
            List<Message> messages = Arrays.asList(sampleMessage);
            when(messageRepository.findBySenderAndReceiverOrderByCreatedAtDesc("testSender", "testReceiver"))
                    .thenReturn(messages);

            // when
            List<MessageDTO> result = messageService.getMessagesBySenderAndReceiver("testSender", "testReceiver");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSender()).isEqualTo("testSender");
            assertThat(result.get(0).getReceiver()).isEqualTo("testReceiver");

            verify(messageRepository).findBySenderAndReceiverOrderByCreatedAtDesc("testSender", "testReceiver");
        }

        @Test
        @DisplayName("成功获取所有消息")
        void shouldGetAllMessagesSuccessfully() {
            // given
            List<Message> messages = Arrays.asList(sampleMessage);
            when(messageRepository.findAllByOrderByCreatedAtDesc()).thenReturn(messages);

            // when
            List<MessageDTO> result = messageService.getAllMessages();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo("msg123");

            verify(messageRepository).findAllByOrderByCreatedAtDesc();
        }
    }

    @Nested
    @DisplayName("消息状态管理测试")
    class MessageStatusTests {

        @Test
        @DisplayName("成功标记消息为已读")
        void shouldMarkMessageAsReadSuccessfully() {
            // given
            Message unreadMessage = Message.builder()
                    .id("msg123")
                    .title("测试消息")
                    .content("内容")
                    .sender("testSender")
                    .receiver("testReceiver")
                    .status("未读")
                    .build();

            when(messageRepository.findById("msg123")).thenReturn(Optional.of(unreadMessage));
            when(messageRepository.save(any(Message.class))).thenReturn(unreadMessage);

            // when
            MessageDTO result = messageService.markAsRead("msg123");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("已读");

            verify(messageRepository).findById("msg123");
            verify(messageRepository).save(argThat(msg -> 
                "已读".equals(msg.getStatus()) && msg.getUpdatedAt() != null));
        }

        @Test
        @DisplayName("消息不存在时标记已读失败")
        void shouldReturnNullWhenMarkAsReadMessageNotFound() {
            // given
            when(messageRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // when
            MessageDTO result = messageService.markAsRead("nonexistent");

            // then
            assertThat(result).isNull();

            verify(messageRepository).findById("nonexistent");
            verify(messageRepository, never()).save(any(Message.class));
        }
    }

    @Nested
    @DisplayName("消息删除测试")
    class DeleteMessageTests {

        @Test
        @DisplayName("成功删除消息")
        void shouldDeleteMessageSuccessfully() {
            // when
            messageService.deleteMessage("msg123");

            // then
            verify(messageRepository).deleteById("msg123");
        }
    }

    @Nested
    @DisplayName("发送者信息测试")
    class SenderInfoTests {

        @Test
        @DisplayName("成功获取接收者的发送者列表")
        void shouldGetSendersByReceiverSuccessfully() {
            // given
            List<Message> messages = Arrays.asList(sampleMessage);
            when(messageRepository.findByReceiver("testReceiver")).thenReturn(messages);
            when(userRepository.findByUsername("testSender")).thenReturn(Optional.of(senderUser));

            // when
            var result = messageService.getSendersByReceiver("testReceiver");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSenderUsername()).isEqualTo("testSender");
            assertThat(result.get(0).getSenderId()).isEqualTo("sender123");

            verify(messageRepository).findByReceiver("testReceiver");
            verify(userRepository).findByUsername("testSender");
        }
    }

    @Nested
    @DisplayName("角色过滤测试")
    class RoleFilterTests {

        @Test
        @DisplayName("成功根据发送者和角色获取消息")
        void shouldGetMessagesBySenderAndRoleSuccessfully() {
            // given
            List<Message> messages = Arrays.asList(sampleMessage);
            when(messageRepository.findBySenderAndSenderRoleOrderByCreatedAtDesc("testSender", "TEACHER"))
                    .thenReturn(messages);

            // when
            List<MessageDTO> result = messageService.getMessagesBySenderAndRole("testSender", "TEACHER");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSender()).isEqualTo("testSender");
            assertThat(result.get(0).getSenderRole()).isEqualTo("TEACHER");

            verify(messageRepository).findBySenderAndSenderRoleOrderByCreatedAtDesc("testSender", "TEACHER");
        }
    }

    @Nested
    @DisplayName("通知发送测试")
    class NotificationTests {

        @Test
        @DisplayName("成功发送系统通知")
        void shouldSendSystemNotificationSuccessfully() {
            // given
            String title = "系统通知";
            String content = "系统维护通知";
            List<String> receiverIds = Arrays.asList("user1", "user2", "user3");

            when(messageRepository.save(any(Message.class))).thenReturn(sampleMessage);

            // when
            messageService.sendSystemNotification(title, content, receiverIds);

            // then
            verify(messageRepository, times(3)).save(argThat(msg ->
                    msg.getTitle().equals(title) &&
                    msg.getContent().equals(content) &&
                    msg.getSender().equals("SYSTEM") &&
                    msg.getSenderRole().equals("SYSTEM") &&
                    msg.getStatus().equals("未读")
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
                    .thenReturn(sampleMessage)  // user1 成功
                    .thenThrow(new RuntimeException("数据库错误"))  // user2 失败
                    .thenReturn(sampleMessage); // user3 成功

            // when
            assertThatNoException().isThrownBy(() ->
                    messageService.sendSystemNotification(title, content, receiverIds));

            // then
            verify(messageRepository, times(3)).save(any(Message.class));
        }

        @Test
        @DisplayName("成功发送实验通知")
        void shouldSendExperimentNotificationSuccessfully() {
            // given
            String title = "实验通知";
            String content = "新实验发布";
            String experimentId = "exp123";
            List<String> receiverIds = Arrays.asList("student1", "student2");

            when(messageRepository.save(any(Message.class))).thenReturn(sampleMessage);

            // when
            messageService.sendExperimentNotification(title, content, experimentId, receiverIds);

            // then
            verify(messageRepository, times(2)).save(argThat(msg ->
                    msg.getTitle().equals(title) &&
                    msg.getContent().equals(content) &&
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
                    .thenReturn(sampleMessage)  // student1 成功
                    .thenThrow(new RuntimeException("保存失败"))  // student2 失败
                    .thenReturn(sampleMessage); // student3 成功

            // when
            assertThatNoException().isThrownBy(() ->
                    messageService.sendExperimentNotification(title, content, experimentId, receiverIds));

            // then
            verify(messageRepository, times(3)).save(any(Message.class));
        }

        @Test
        @DisplayName("成功发送成绩通知")
        void shouldSendGradeNotificationSuccessfully() {
            // given
            String title = "成绩通知";
            String content = "实验成绩已发布";
            String experimentId = "exp123";
            String receiverId = "student1";

            when(messageRepository.save(any(Message.class))).thenReturn(sampleMessage);

            // when
            messageService.sendGradeNotification(title, content, experimentId, receiverId);

            // then
            verify(messageRepository).save(argThat(msg ->
                    msg.getTitle().equals(title) &&
                    msg.getContent().equals(content) &&
                    msg.getReceiver().equals(receiverId) &&
                    msg.getSender().equals("SYSTEM") &&
                    msg.getSenderRole().equals("SYSTEM")
            ));
        }

        @Test
        @DisplayName("发送成绩通知时保存失败抛出异常")
        void shouldThrowExceptionWhenGradeNotificationSaveFails() {
            // given
            String title = "成绩通知";
            String content = "实验成绩已发布";
            String experimentId = "exp123";
            String receiverId = "student1";

            when(messageRepository.save(any(Message.class)))
                    .thenThrow(new RuntimeException("数据库保存失败"));

            // when & then
            assertThatThrownBy(() -> 
                    messageService.sendGradeNotification(title, content, experimentId, receiverId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("发送成绩通知失败");

            verify(messageRepository).save(any(Message.class));
        }
    }
}
