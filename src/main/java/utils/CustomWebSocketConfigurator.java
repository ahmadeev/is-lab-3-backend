package utils;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.List;

public class CustomWebSocketConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        // Получаем заголовок Origin
        List<String> origins = request.getHeaders().get("Origin");

        if (origins != null && !origins.isEmpty()) {
            String origin = origins.get(0);
            System.out.println("[WS] Received Origin: " + origin);
            // Логируем Origin для отладки
        } else {
            System.out.println("[WS] Missing Origin header. Allowing connection for debugging purposes.");
        }

        // TODO: Разрешаем все Origin (временно для отладки)
        response.getHeaders().put("Access-Control-Allow-Origin", List.of("*"));

        // Вы можете добавить пользовательские атрибуты в конфигурацию, если нужно
        // config.getUserProperties().put("customAttribute", "customValue");
    }
}

