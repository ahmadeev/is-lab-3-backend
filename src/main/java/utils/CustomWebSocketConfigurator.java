package utils;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.List;

public class CustomWebSocketConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        // получаем заголовок Origin
        List<String> origins = request.getHeaders().get("Origin");

        if (origins != null && !origins.isEmpty()) {
            String origin = origins.get(0);
            System.out.println("[WS] Received Origin: " + origin);
            // логируем Origin для отладки
        } else {
            System.out.println("[WS] Missing Origin header. Allowing connection for debugging purposes.");
        }

        // TODO: разрешаем все Origin (временно для отладки)
        response.getHeaders().put("Access-Control-Allow-Origin", List.of("*"));

        // пользовательские атрибуты
        // config.getUserProperties().put("customAttribute", "customValue");
    }
}

