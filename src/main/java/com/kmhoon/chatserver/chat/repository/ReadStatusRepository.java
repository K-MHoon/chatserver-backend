package com.kmhoon.chatserver.chat.repository;

import com.kmhoon.chatserver.chat.domain.ChatRoom;
import com.kmhoon.chatserver.chat.domain.ReadStatus;
import com.kmhoon.chatserver.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadStatusRepository extends JpaRepository<ReadStatus, Long> {

    @Modifying
    @Query("update ReadStatus rs set rs.isRead = ?1 where rs.chatRoom = ?2 and rs.member = ?3")
    int updateIsRead(boolean isRead, ChatRoom chatRoom, Member member);

    List<ReadStatus> findAllByChatRoomInAndMemberAndIsReadFalse(List<ChatRoom> chatRooms, Member member);
}
