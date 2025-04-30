package com.kmhoon.chatserver.chat.service;

import com.kmhoon.chatserver.chat.domain.ChatMessage;
import com.kmhoon.chatserver.chat.domain.ChatParticipant;
import com.kmhoon.chatserver.chat.domain.ChatRoom;
import com.kmhoon.chatserver.chat.domain.ReadStatus;
import com.kmhoon.chatserver.chat.dto.ChatMessageDto;
import com.kmhoon.chatserver.chat.dto.ChatRoomListResDto;
import com.kmhoon.chatserver.chat.dto.MyChatListResDto;
import com.kmhoon.chatserver.chat.repository.ChatMessageRepository;
import com.kmhoon.chatserver.chat.repository.ChatParticipantRepository;
import com.kmhoon.chatserver.chat.repository.ChatRoomRepository;
import com.kmhoon.chatserver.chat.repository.ReadStatusRepository;
import com.kmhoon.chatserver.member.domain.Member;
import com.kmhoon.chatserver.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.hibernate.internal.util.collections.CollectionHelper.toMap;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ReadStatusRepository readStatusRepository;
    private final MemberRepository memberRepository;

    public void saveMessage(Long roomId, ChatMessageDto reqDto) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("room can't be found"));

        // 보낸사람 조회
        Member sender = memberRepository.findByEmail(reqDto.getSenderEmail()).orElseThrow(() -> new EntityNotFoundException("member can't be found"));

        // 메시지 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .member(sender)
                .content(reqDto.getMessage())
                .build();
        ChatMessage newChatMessage = chatMessageRepository.save(chatMessage);

        // 사용자별로 읽음여부 저장
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        chatParticipants.stream().map(rs ->
                        ReadStatus.builder().chatRoom(chatRoom)
                                .member(rs.getMember())
                                .chatMessage(newChatMessage)
                                .isRead(rs.getMember().equals(sender))
                                .build())
                .forEach(readStatusRepository::save);
    }

    public void createGroupRoom(String chatRoomName) {
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(() -> new EntityNotFoundException("member can't be found"));

        // 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .name(chatRoomName)
                .isGroupChat("Y")
                .build();

        ChatRoom createdChatRoom = chatRoomRepository.save(chatRoom);

        // 채팅참여자로 개설자를 추가
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(createdChatRoom)
                .member(member)
                .build();

        chatParticipantRepository.save(chatParticipant);
    }

    public List<ChatRoomListResDto> getGroupChatRooms() {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllByIsGroupChat("Y");
        return chatRooms.stream().map(c ->
                        ChatRoomListResDto.builder()
                                .roomId(c.getId())
                                .roomName(c.getName())
                                .build())
                .toList();
    }

    public void addParticipantToGroupChat(Long roomId) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("room can't be found"));
        // member 조회
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("member can't be found"));
        if(chatRoom.getIsGroupChat().equals("N")) {
            throw new IllegalArgumentException("그룹채팅이 아닙니다.");
        }

        // 이미 참여자인지 검증
        Optional<ChatParticipant> participant = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member);
        if (participant.isPresent()) {
            return;
        }
        addParticipantToChatRoom(chatRoom, member);
    }

    public void addParticipantToChatRoom(ChatRoom chatRoom, Member member) {
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();
        chatParticipantRepository.save(chatParticipant);
    }

    public List<ChatMessageDto> getChatHistory(Long roomId) {
        // 내가 해당 채팅방의 참여자가 아닐경우 에러
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("room can't be found"));
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("member can't be found"));
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        if (chatParticipants.stream().noneMatch(c -> c.getMember().equals(member))) {
            throw new IllegalArgumentException("본인이 속하지 않은 채팅방입니다.");
        }

        // 특정 room에 대한 message 조회
        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomOrderByCreatedTimeAsc(chatRoom);
        return chatMessages.stream().map(c -> ChatMessageDto.builder()
                        .message(c.getContent())
                        .senderEmail(c.getMember().getEmail())
                        .build())
                .toList();
    }

    public boolean isRoomParticipant(String email, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("room can't be found"));
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("member can't be found"));

        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        return chatParticipants.stream().anyMatch(c -> c.getMember().equals(member));
    }

    public void messageRead(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("room can't be found"));
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("member can't be found"));
        readStatusRepository.updateIsRead(true, chatRoom, member);
    }

    public List<MyChatListResDto> getMyChatRooms() {
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("member can't be found"));
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findAllByMember(member);
        List<ChatRoom> chatRooms = chatParticipants.stream().map(ChatParticipant::getChatRoom).toList();
        List<ReadStatus> readStatuses = readStatusRepository.findAllByChatRoomInAndMemberAndIsReadFalse(chatRooms, member);
        Map<ChatRoom, Long> chatRoomToCountMap = readStatuses.stream().collect(groupingBy(ReadStatus::getChatRoom, counting()));
        return chatParticipants.stream().map(cp -> MyChatListResDto.builder()
                        .roomId(cp.getChatRoom().getId())
                        .roomName(cp.getChatRoom().getName())
                        .isGroupChat(cp.getChatRoom().getIsGroupChat())
                        .unReadCount(chatRoomToCountMap.getOrDefault(cp.getChatRoom(), 0L))
                        .build())
                .toList();
    }

    public void leaveGroupChatRoom(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("room can't be found"));
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("member can't be found"));
        if(chatRoom.getIsGroupChat().equals("N")) {
            throw new IllegalArgumentException("단체 채팅방이 아닙니다.");
        }
        ChatParticipant chatParticipant = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member).orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다."));
        chatParticipantRepository.delete(chatParticipant);

        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        if(chatParticipants.isEmpty()) {
            chatRoomRepository.delete(chatRoom);
        }
    }

    public Long getOrCreatePrivateRoom(Long otherMemberId) {
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(() -> new EntityNotFoundException("member can't be found"));
        Member otherMember = memberRepository.findById(otherMemberId).orElseThrow(() -> new EntityNotFoundException("other member can't be found"));

        // 나와 상대방이 1:1채팅에 이미 참석하고 있다면 해당 roomId return
        Optional<ChatRoom> chatRoom = chatParticipantRepository.findByExistingPrivateRoom(member.getId(), otherMember.getId());
        if(chatRoom.isPresent()) {
            return chatRoom.get().getId();
        }
        // 만약 1:1 채팅방이 없을경우 기존 채팅방 개설
        ChatRoom newRoom = ChatRoom.builder()
                .isGroupChat("N")
                .name(member.getName() + "-" + otherMember.getName())
                .build();
        ChatRoom savedNewRoom = chatRoomRepository.save(newRoom);
        // 두사람 모두 참여자로 새롭게 추가
        addParticipantToChatRoom(savedNewRoom, member);
        addParticipantToChatRoom(savedNewRoom, otherMember);

        return savedNewRoom.getId();
    }
}
