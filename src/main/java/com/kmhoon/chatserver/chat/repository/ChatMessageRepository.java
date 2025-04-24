package com.kmhoon.chatserver.chat.repository;

import com.kmhoon.chatserver.chat.domain.ChatMessage;
import com.kmhoon.chatserver.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @EntityGraph(attributePaths = {"member"})
    List<ChatMessage> findByChatRoomOrderByCreatedTimeAsc(ChatRoom chatRoom);
}
