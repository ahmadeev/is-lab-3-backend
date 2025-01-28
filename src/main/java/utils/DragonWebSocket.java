package utils;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ServerEndpoint(value = "/ws/dragons", configurator = CustomWebSocketConfigurator.class)
public class DragonWebSocket {

    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("[WS] Session opened: " + session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("[WS] Error: " + throwable.getMessage());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("[WS] Session closed: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // Обработка входящих сообщений (если нужно)
        System.out.println("[WS] Received message (session: " + session + "): " + message);
    }

    public static void broadcast(String message) {
        for (Session session : sessions) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(message);
            }
        }
    }
}
