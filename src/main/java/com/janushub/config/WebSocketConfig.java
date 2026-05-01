package com.janushub.config;

import com.janushub.websocket.LiveEstimationWsHandler;
import com.janushub.websocket.NotificationsWsHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private LiveEstimationWsHandler liveEstimationWsHandler;

    @Autowired
    private NotificationsWsHandler notificationsWsHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveEstimationWsHandler, "/ws/live-estimation")
                .setAllowedOriginPatterns("*");

        registry.addHandler(notificationsWsHandler, "/ws/notifications")
                .setAllowedOriginPatterns("*");
    }
}
