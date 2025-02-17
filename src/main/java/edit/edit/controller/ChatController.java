package edit.edit.controller;

import edit.edit.dto.ResponseDto;
import edit.edit.dto.chat.ChatDto;
import edit.edit.dto.chat.ChatRoomIdRequestDto;
import edit.edit.dto.chat.ChatRoomIdResponseDto;
import edit.edit.dto.chat.ChatRoomRequestDto;
import edit.edit.security.UserDetailsImpl;
import edit.edit.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;
	private final SimpMessagingTemplate msgTemplate;


	@PostMapping("/chat")
	@ResponseBody
	public ResponseDto createChatRoom(@RequestBody ChatRoomRequestDto chatRoomRequestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
		return chatService.createChatRoom(chatRoomRequestDto, userDetails.getMember());
		// createChatRoom의 결과인 roomId와 type : ENTER을 저장한 chatDto에 넣어줘야함
	}

	@ResponseBody
	@PostMapping("/chat/find")
	public ResponseDto findChatRoom(@RequestBody ChatRoomRequestDto chatRoomRequestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
		return chatService.findChatRoom(chatRoomRequestDto, userDetails.getMember());
	}

	@MessageMapping("/chat/enter")
	@SendTo("/sub/chat/room")
	public void enterChatRoom(ChatDto chatDto, SimpMessageHeaderAccessor headerAccessor) throws Exception {
		Thread.sleep(500); // simulated delay
		ChatDto newchatdto = chatService.enterChatRoom(chatDto, headerAccessor);

		msgTemplate.convertAndSend("/sub/chat/room" + chatDto.getRoomId(), newchatdto);
	}

	@MessageMapping("/chat/send")
	@SendTo("/sub/chat/room")
	public void sendChatRoom(ChatDto chatDto) throws Exception {
		Thread.sleep(500); // simulated delay
		msgTemplate.convertAndSend("/sub/chat/room" + chatDto.getRoomId(), chatDto);
	}

	@MessageMapping("/chat/subscribe")
	@SendTo("/sub/chat/queue")
	public void subscribeUser(ChatRoomIdRequestDto chatRoomIdRequestDto) throws Exception {
		Thread.sleep(500); // simulated delay
		msgTemplate.convertAndSend("/sub/chat/queue" + chatRoomIdRequestDto.getReceiver(), new ChatRoomIdResponseDto(chatRoomIdRequestDto.getRoomId(), chatRoomIdRequestDto.getSender()));
	}

	@EventListener
	public void webSocketDisconnectListener(SessionDisconnectEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
		ChatDto chatDto = chatService.disconnectChatRoom(headerAccessor);
		msgTemplate.convertAndSend("/sub/chat/room" + chatDto.getRoomId(), chatDto);
	}
}
