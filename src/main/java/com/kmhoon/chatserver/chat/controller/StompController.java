package com.kmhoon.chatserver.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kmhoon.chatserver.chat.dto.ChatMessageDto;
import com.kmhoon.chatserver.chat.service.ChatService;
import com.kmhoon.chatserver.chat.service.RedisPubSubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

@Controller
@Slf4j
@RequiredArgsConstructor
public class StompController {

    private final SimpMessageSendingOperations messageTemplate;
    private final ChatService chatService;
    private final RedisPubSubService pubSubService;
    private final ObjectMapper objectMapper;

    // 방법1. MessageMapping(수신)과 SendTo(topic에 메시지전달)한꺼번에 처리
//    @MessageMapping("/{roomId}") // 클라이언트에서 특정 publish/roomId 형태로 메시지를 발행시 MessageMapping 수신
//    @SendTo("/topic/{roomId}") // 해당 roomId에 메시지를 발행하여 구독중인 클라이언트에게 메시지 전송
//    // DestinationVariable @MessageMapping 어노테이션으로 정의된 WebSocket Controller 내에서만 사용한다.
//    public String sendMessage(@DestinationVariable Long roomId, String message) {
//        log.info("message = {}", message);
//        return message;
//    }

    // 방법2. MessageMapping 어노테이션만 활용 (유연성을 위해)
    @MessageMapping("/{roomId}") // 클라이언트에서 특정 publish/roomId 형태로 메시지를 발행시 MessageMapping 수신
    // DestinationVariable @MessageMapping 어노테이션으로 정의된 WebSocket Controller 내에서만 사용한다.
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageDto reqDto) throws JsonProcessingException {
        log.info("message = {}", reqDto.getMessage());
        reqDto.setRoomId(roomId);
        // @SendTo와 동일한 효과, 나중에 변경사항이 생겼을 때 해당 형식이 더 유연하게 사용 가능하다.
        chatService.saveMessage(roomId, reqDto);
        String message = objectMapper.writeValueAsString(reqDto);
        pubSubService.publish("chat", message);
    }

}
