package org.linghu.mybackend.repository;

import java.util.List;

import org.linghu.mybackend.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 消息通知数据访问层
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findByReceiver(String receiver);
    //List<Message> findBySender(String sender);
    //List<Message> findBySenderAndReceiver(String sender, String receiver);
    List<Message> findByReceiverOrderByCreatedAtDesc(String receiver);
    List<Message> findBySenderAndReceiverOrderByCreatedAtDesc(String sender, String receiver);
    List<Message> findBySenderAndSenderRoleOrderByCreatedAtDesc(String sender, String senderRole);
    List<Message> findAllByOrderByCreatedAtDesc();
}
