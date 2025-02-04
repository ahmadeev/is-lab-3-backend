package utils;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ServerEndpoint(value = "/ws/dragons", configurator = CustomWebSocketConfigurator.class)
public class DragonWebSocket {

    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private static final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("[WS] Session opened: " + session.getId());

        pingScheduler.scheduleAtFixedRate(() -> sendPing(session), 30, 30, TimeUnit.SECONDS);
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

    private void sendPing(Session session) {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendPing(ByteBuffer.wrap(new byte[0]));
            }
        } catch (Exception e) {
            System.err.println("[WS] Ping error: " + e.getMessage());
        }
    }
}
