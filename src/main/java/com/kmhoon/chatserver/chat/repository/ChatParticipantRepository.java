package com.kmhoon.chatserver.chat.repository;

import com.kmhoon.chatserver.chat.domain.ChatParticipant;
import com.kmhoon.chatserver.chat.domain.ChatRoom;
import com.kmhoon.chatserver.member.domain.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    List<ChatParticipant> findByChatRoom(ChatRoom chatRoom);

    Optional<ChatParticipant> findByChatRoomAndMember(ChatRoom chatRoom, Member member);

    @EntityGraph(attributePaths = {"chatRoom"})
    List<ChatParticipant> findAllByMember(Member member);

    @Query("select cp1.chatRoom " +
            "from ChatParticipant cp1 join ChatParticipant cp2 " +
            "on cp1.chatRoom.id = cp2.chatRoom.id " +
            "where cp1.member.id = ?1 " +
            "and cp2.member.id = ?2 " +
            "and cp1.chatRoom.isGroupChat = 'N'")
    Optional<ChatRoom> findByExistingPrivateRoom(Long myId, Long otherMemberId);
}
