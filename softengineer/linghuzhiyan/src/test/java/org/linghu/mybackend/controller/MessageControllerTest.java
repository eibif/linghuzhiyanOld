package org.linghu.mybackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.dto.MessageDTO;
import org.linghu.mybackend.dto.MessageRequestDTO;
import org.linghu.mybackend.dto.SenderInfoDTO;
import org.linghu.mybackend.dto.UserDTO;
import org.linghu.mybackend.service.MessageService;
import org.linghu.mybackend.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageController 单元测试")
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private UserService userService;

    @InjectMocks
    private MessageController messageController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // 设置请求上下文
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    @Nested
    @DisplayName("创建消息测试")
    class CreateMessageTests {

        @Test
        @DisplayName("管理员成功创建消息")
        void createMessage_AdminUser_Success() throws Exception {
            // Arrange
            UserDetails adminUser = User.builder()
                    .username("admin")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            MessageRequestDTO request = MessageRequestDTO.builder()
                    .title("测试消息")
                    .content("测试内容")
                    .receiver("student1")
                    .build();

            UserDTO receiverUser = UserDTO.builder()
                    .id("receiver-id")
                    .username("student1")
                    .build();

            MessageDTO expectedMessage = MessageDTO.builder()
                    .id("msg-id")
                    .title("测试消息")
                    .content("测试内容")
                    .sender("admin")
                    .receiver("student1")
                    .senderRole("ROLE_ADMIN")
                    .status("UNREAD")
                    .build();

            when(userService.getUserByUsername("student1")).thenReturn(receiverUser);
            when(userService.getUserRoleIds("receiver-id"))
                    .thenReturn(Set.of("ROLE_STUDENT"));
            when(messageService.createMessage(any(MessageDTO.class))).thenReturn(expectedMessage);

            // Act
            var result = messageController.createMessage(request, adminUser);

            // Assert
            assertNotNull(result);
            assertEquals("测试消息", result.getData().getTitle());
            assertEquals("测试内容", result.getData().getContent());
            assertEquals("admin", result.getData().getSender());
            assertEquals("student1", result.getData().getReceiver());
            assertEquals("ROLE_ADMIN", result.getData().getSenderRole());
        }

        @Test
        @DisplayName("教师成功向学生创建消息")
        void createMessage_TeacherToStudent_Success() throws Exception {
            // Arrange
            UserDetails teacherUser = User.builder()
                    .username("teacher1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
                    .build();

            MessageRequestDTO request = MessageRequestDTO.builder()
                    .title("作业通知")
                    .content("请及时完成作业")
                    .receiver("student1")
                    .build();

            UserDTO receiverUser = UserDTO.builder()
                    .id("receiver-id")
                    .username("student1")
                    .build();

            MessageDTO expectedMessage = MessageDTO.builder()
                    .id("msg-id")
                    .title("作业通知")
                    .content("请及时完成作业")
                    .sender("teacher1")
                    .receiver("student1")
                    .senderRole("ROLE_TEACHER")
                    .status("UNREAD")
                    .build();

            when(userService.getUserByUsername("student1")).thenReturn(receiverUser);
            when(userService.getUserRoleIds("receiver-id"))
                    .thenReturn(Set.of("ROLE_STUDENT"));
            when(messageService.createMessage(any(MessageDTO.class))).thenReturn(expectedMessage);

            // Act
            var result = messageController.createMessage(request, teacherUser);

            // Assert
            assertNotNull(result);
            assertEquals("作业通知", result.getData().getTitle());
            assertEquals("ROLE_TEACHER", result.getData().getSenderRole());
        }

        @Test
        @DisplayName("学生尝试向管理员发送消息失败")
        void createMessage_StudentToAdmin_Forbidden() throws Exception {
            // Arrange
            UserDetails studentUser = User.builder()
                    .username("student1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            MessageRequestDTO request = MessageRequestDTO.builder()
                    .title("申请")
                    .content("申请内容")
                    .receiver("admin")
                    .build();

            UserDTO receiverUser = UserDTO.builder()
                    .id("admin-id")
                    .username("admin")
                    .build();

            when(userService.getUserByUsername("admin")).thenReturn(receiverUser);
            when(userService.getUserRoleIds("admin-id"))
                    .thenReturn(Set.of("ROLE_ADMIN"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                messageController.createMessage(request, studentUser);
            });
        }

        @Test
        @DisplayName("接收者不存在时创建消息失败")
        void createMessage_ReceiverNotFound_Failure() throws Exception {
            // Arrange
            UserDetails adminUser = User.builder()
                    .username("admin")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            MessageRequestDTO request = MessageRequestDTO.builder()
                    .title("测试消息")
                    .content("测试内容")
                    .receiver("nonexistent")
                    .build();

            when(userService.getUserByUsername("nonexistent")).thenReturn(null);

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                messageController.createMessage(request, adminUser);
            });
        }

        @Test
        @DisplayName("未登录用户创建消息失败")
        void createMessage_NoAuth_Failure() throws Exception {
            // Arrange
            MessageRequestDTO request = MessageRequestDTO.builder()
                    .title("测试消息")
                    .content("测试内容")
                    .receiver("student1")
                    .build();

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                messageController.createMessage(request, null);
            });
        }
    }

    @Nested
    @DisplayName("获取消息测试")
    class GetMessageTests {

        @Test
        @DisplayName("根据ID获取消息成功")
        void getMessageById_Success() throws Exception {
            // Arrange
            String messageId = "msg-123";
            MessageDTO message = MessageDTO.builder()
                    .id(messageId)
                    .title("测试消息")
                    .content("测试内容")
                    .sender("teacher1")
                    .receiver("student1")
                    .status("UNREAD")
                    .build();

            when(messageService.getMessageById(messageId)).thenReturn(message);

            // Act
            var result = messageController.getMessageById(messageId);

            // Assert
            assertNotNull(result);
            assertEquals(messageId, result.getData().getId());
            assertEquals("测试消息", result.getData().getTitle());
            assertEquals("测试内容", result.getData().getContent());
        }

        @Test
        @DisplayName("获取当前用户接收的消息成功")
        void getMessagesByReceiver_Success() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("student1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            List<MessageDTO> messages = Arrays.asList(
                    MessageDTO.builder()
                            .id("msg-1")
                            .title("消息1")
                            .sender("teacher1")
                            .receiver("student1")
                            .build(),
                    MessageDTO.builder()
                            .id("msg-2")
                            .title("消息2")
                            .sender("admin")
                            .receiver("student1")
                            .build()
            );

            when(messageService.getMessagesByReceiver("student1")).thenReturn(messages);

            // Act
            var result = messageController.getMessagesByReceiver(user);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getData().size());
            assertEquals("消息1", result.getData().get(0).getTitle());
            assertEquals("消息2", result.getData().get(1).getTitle());
        }

        @Test
        @DisplayName("获取指定发送者的消息成功")
        void getMessagesBySender_Success() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("student1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            String sender = "teacher1";
            List<MessageDTO> messages = Arrays.asList(
                    MessageDTO.builder()
                            .id("msg-1")
                            .title("作业通知")
                            .sender(sender)
                            .receiver("student1")
                            .build()
            );

            when(messageService.getMessagesBySenderAndReceiver(sender, "student1"))
                    .thenReturn(messages);

            // Act
            var result = messageController.getMessagesBySender(sender, user);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getData().size());
            assertEquals("作业通知", result.getData().get(0).getTitle());
        }

        @Test
        @DisplayName("获取发送者列表成功")
        void getSendersByReceiver_Success() throws Exception {
            // Arrange
            UserDetails user = User.builder()
                    .username("student1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .build();

            List<SenderInfoDTO> senders = Arrays.asList(
                    SenderInfoDTO.builder()
                            .senderId("teacher-id")
                            .senderUsername("teacher1")
                            .senderRole("ROLE_TEACHER")
                            .build(),
                    SenderInfoDTO.builder()
                            .senderId("admin-id")
                            .senderUsername("admin")
                            .senderRole("ROLE_ADMIN")
                            .build()
            );

            when(messageService.getSendersByReceiver("student1")).thenReturn(senders);

            // Act
            var result = messageController.getSendersByReceiver(user);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getData().size());
            assertEquals("teacher1", result.getData().get(0).getSenderUsername());
            assertEquals("admin", result.getData().get(1).getSenderUsername());
        }

        @Test
        @DisplayName("获取所有消息成功")
        void getAllMessages_Success() throws Exception {
            // Arrange
            List<MessageDTO> messages = Arrays.asList(
                    MessageDTO.builder()
                            .id("msg-1")
                            .title("系统消息1")
                            .build(),
                    MessageDTO.builder()
                            .id("msg-2")
                            .title("系统消息2")
                            .build()
            );

            when(messageService.getAllMessages()).thenReturn(messages);

            // Act
            var result = messageController.getAllMessages();

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getData().size());
        }

        @Test
        @DisplayName("获取自己发送的消息成功")
        void getSelfSentMessages_Success() throws Exception {
            // Arrange
            UserDetails teacherUser = User.builder()
                    .username("teacher1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
                    .build();

            List<MessageDTO> sentMessages = Arrays.asList(
                    MessageDTO.builder()
                            .id("msg-1")
                            .title("发送的消息1")
                            .sender("teacher1")
                            .senderRole("ROLE_TEACHER")
                            .build()
            );

            when(messageService.getMessagesBySenderAndRole("teacher1", "ROLE_TEACHER"))
                    .thenReturn(sentMessages);

            // Act
            var result = messageController.getSelfSentMessages(teacherUser);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getData().size());
            assertEquals("发送的消息1", result.getData().get(0).getTitle());
        }
    }

    @Nested
    @DisplayName("消息状态管理测试")
    class MessageStatusTests {

        @Test
        @DisplayName("标记消息为已读成功")
        void markAsRead_Success() throws Exception {
            // Arrange
            String messageId = "msg-123";
            MessageDTO readMessage = MessageDTO.builder()
                    .id(messageId)
                    .title("测试消息")
                    .status("READ")
                    .build();

            when(messageService.markAsRead(messageId)).thenReturn(readMessage);

            // Act
            var result = messageController.markAsRead(messageId);

            // Assert
            assertNotNull(result);
            assertEquals(messageId, result.getData().getId());
            assertEquals("READ", result.getData().getStatus());
        }

        @Test
        @DisplayName("删除消息成功")
        void deleteMessage_Success() throws Exception {
            // Arrange
            String messageId = "msg-123";

            // Act
            var result = messageController.deleteMessage(messageId);

            // Assert
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("权限验证测试")
    class AuthorizationTests {

        @Test
        @DisplayName("助教只能向助教或学生发送消息")
        void createMessage_AssistantToTeacher_Forbidden() throws Exception {
            // Arrange
            UserDetails assistantUser = User.builder()
                    .username("assistant1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_ASSISTANT")))
                    .build();

            MessageRequestDTO request = MessageRequestDTO.builder()
                    .title("申请")
                    .content("申请内容")
                    .receiver("teacher1")
                    .build();

            UserDTO receiverUser = UserDTO.builder()
                    .id("teacher-id")
                    .username("teacher1")
                    .build();

            when(userService.getUserByUsername("teacher1")).thenReturn(receiverUser);
            when(userService.getUserRoleIds("teacher-id"))
                    .thenReturn(Set.of("ROLE_TEACHER"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                messageController.createMessage(request, assistantUser);
            });
        }

        @Test
        @DisplayName("教师不能向管理员发送消息")
        void createMessage_TeacherToAdmin_Forbidden() throws Exception {
            // Arrange
            UserDetails teacherUser = User.builder()
                    .username("teacher1")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
                    .build();

            MessageRequestDTO request = MessageRequestDTO.builder()
                    .title("申请")
                    .content("申请内容")
                    .receiver("admin")
                    .build();

            UserDTO receiverUser = UserDTO.builder()
                    .id("admin-id")
                    .username("admin")
                    .build();

            when(userService.getUserByUsername("admin")).thenReturn(receiverUser);
            when(userService.getUserRoleIds("admin-id"))
                    .thenReturn(Set.of("ROLE_ADMIN"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                messageController.createMessage(request, teacherUser);
            });
        }
    }
}
