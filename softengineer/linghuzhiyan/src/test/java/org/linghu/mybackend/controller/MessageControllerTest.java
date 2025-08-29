package org.linghu.mybackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linghu.mybackend.controller.MessageController;
import org.linghu.mybackend.dto.MessageDTO;
import org.linghu.mybackend.dto.MessageRequestDTO;
import org.linghu.mybackend.dto.SenderInfoDTO;
import org.linghu.mybackend.dto.UserDTO;
import org.linghu.mybackend.service.MessageService;
import org.linghu.mybackend.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageController 单元测试")
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private UserService userService;

    @InjectMocks
    private MessageController messageController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messageController).build();
        objectMapper = new ObjectMapper();
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

            // Act & Assert
            mockMvc.perform(post("/api/messages")
                            .with(user(adminUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("测试消息"))
                    .andExpect(jsonPath("$.data.content").value("测试内容"))
                    .andExpect(jsonPath("$.data.sender").value("admin"))
                    .andExpect(jsonPath("$.data.receiver").value("student1"))
                    .andExpect(jsonPath("$.data.senderRole").value("ROLE_ADMIN"));
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

            // Act & Assert
            mockMvc.perform(post("/api/messages")
                            .with(user(teacherUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("作业通知"))
                    .andExpect(jsonPath("$.data.senderRole").value("ROLE_TEACHER"));
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
            mockMvc.perform(post("/api/messages")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
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
            mockMvc.perform(post("/api/messages")
                            .with(user(adminUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
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
            mockMvc.perform(post("/api/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
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

            // Act & Assert
            mockMvc.perform(get("/api/messages/{id}", messageId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(messageId))
                    .andExpect(jsonPath("$.data.title").value("测试消息"))
                    .andExpect(jsonPath("$.data.content").value("测试内容"));
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

            // Act & Assert
            mockMvc.perform(get("/api/messages/receiver")
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].title").value("消息1"))
                    .andExpect(jsonPath("$.data[1].title").value("消息2"));
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

            // Act & Assert
            mockMvc.perform(get("/api/messages/sender/{sender}", sender)
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("作业通知"));
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

            // Act & Assert
            mockMvc.perform(get("/api/messages/senders")
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].senderUsername").value("teacher1"))
                    .andExpect(jsonPath("$.data[1].senderUsername").value("admin"));
        }

        @Test
        @DisplayName("管理员获取所有消息成功")
        void getAllMessages_AdminUser_Success() throws Exception {
            // Arrange
            UserDetails adminUser = User.builder()
                    .username("admin")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

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

            // Act & Assert
            mockMvc.perform(get("/api/messages/all")
                            .with(user(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));
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

            // Act & Assert
            mockMvc.perform(get("/api/messages/self/sent")
                            .with(user(teacherUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("发送的消息1"));
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

            // Act & Assert
            mockMvc.perform(put("/api/messages/{id}/read", messageId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(messageId))
                    .andExpect(jsonPath("$.data.status").value("READ"));
        }

        @Test
        @DisplayName("删除消息成功")
        void deleteMessage_Success() throws Exception {
            // Arrange
            String messageId = "msg-123";

            // Act & Assert
            mockMvc.perform(delete("/api/messages/{id}", messageId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
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
            mockMvc.perform(post("/api/messages")
                            .with(user(assistantUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
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
            mockMvc.perform(post("/api/messages")
                            .with(user(teacherUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("未登录用户无法获取消息")
        void getMessagesByReceiver_NoAuth_Failure() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/messages/receiver"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("未登录用户无法获取发送者列表")
        void getSendersByReceiver_NoAuth_Failure() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/messages/senders"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("未登录用户无法获取自己发送的消息")
        void getSelfSentMessages_NoAuth_Failure() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/messages/self/sent"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
