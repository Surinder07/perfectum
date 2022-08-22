package ca.waaw.config;

import ca.waaw.config.applicationconfig.WebSocketConfig;
import ca.waaw.security.jwt.TokenProvider;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Objects;

@SuppressWarnings("unused")
@Configuration
@AllArgsConstructor
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfigurer implements WebSocketMessageBrokerConfigurer {

    private final TokenProvider tokenProvider;

    private final WebSocketConfig webSocketConfig;

    private final Logger log = LogManager.getLogger(WebSocketConfigurer.class);

    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        log.info("Registering web sockets...");
        registry.addEndpoint(webSocketConfig.getConnectionEndpoint())
                .setAllowedOrigins(webSocketConfig.getAllowedOrigins())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(webSocketConfig.getSimpleBroker());
        registry.setApplicationDestinationPrefixes(webSocketConfig.getApplicationDestinationPrefix());
    }

    @Override
    public void configureClientInboundChannel(final ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                System.out.println("Reached security");
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                assert accessor != null;
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = Objects.requireNonNull(accessor.getNativeHeader("access_token")).get(0);
                    log.info("jwt token: " + token);
                    Authentication user = tokenProvider.getAuthentication(token);
                    accessor.setUser(user);
                } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                    if (Objects.nonNull(authentication))
                        log.info("Disconnected Auth : " + authentication.getName());
                    else
                        log.info("Disconnected Session : " + accessor.getSessionId());
                }
                return message;
            }

            @Override
            public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
                StompHeaderAccessor sha = StompHeaderAccessor.wrap(message);

                // ignore non-STOMP messages like heartbeat messages
                if (sha.getCommand() == null) {
                    log.warn("postSend null command");
                    return;
                }

                String sessionId = sha.getSessionId();

                switch (sha.getCommand()) {
                    case CONNECT:
                        log.info("STOMP Connect [sessionId: " + sessionId + "]");
                        break;
                    case CONNECTED:
                        log.info("STOMP Connected [sessionId: " + sessionId + "]");
                        break;
                    case DISCONNECT:
                        log.info("STOMP Disconnect [sessionId: " + sessionId + "]");
                        break;
                    default:
                        break;

                }
            }
        });
    }

}
