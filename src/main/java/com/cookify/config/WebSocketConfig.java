package com.cookify.config;

import com.cookify.chat.ChatTopicAuthorization;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

import java.security.Principal;

/**
 * STOMP-over-WebSocket wiring for the built-in chat: a single endpoint
 * {@code /ws} (SockJS fallback for environments where a raw WebSocket upgrade is blocked),
 * a simple in-memory broker for {@code /topic/**} destinations (server -> client push), and
 * the {@code /app} prefix routing client-sent frames to {@code @MessageMapping} handlers
 * (see {@link com.cookify.chat.ChatStompController}).
 *
 * <p>{@link #configureClientInboundChannel} additionally authorizes SUBSCRIBE frames to
 * {@code /topic/chat.*} destinations: a message broker's SUBSCRIBE is not otherwise checked
 * against the per-user {@code anyRequest().authenticated()} HTTP rule, so without this, any
 * authenticated user could subscribe to any OTHER pair's conversation topic and read their
 * messages. {@link ChatTopicAuthorization#isParticipant} is the pure, unit-tested decision;
 * this class is just the thin Spring-messaging wiring around it.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UserRepository users;

    public WebSocketConfig(UserRepository users) {
        this.users = users;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    if (destination != null && destination.startsWith("/topic/chat.")) {
                        Long userId = resolveUserId(accessor.getUser());
                        if (!ChatTopicAuthorization.isParticipant(destination, userId)) {
                            throw new MessageDeliveryException(
                                "Not authorized to subscribe to " + destination);
                        }
                    }
                }
                return message;
            }
        });
    }

    private Long resolveUserId(Principal principal) {
        if (principal == null) {
            return null;
        }
        return users.findByUsername(principal.getName()).map(User::getId).orElse(null);
    }
}
