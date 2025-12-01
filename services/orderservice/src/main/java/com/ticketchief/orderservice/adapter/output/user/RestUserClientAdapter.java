package com.ticketchief.orderservice.adapter.output.user;

import com.ticketchief.orderservice.port.output.UserClientPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class RestUserClientAdapter implements UserClientPort {

    private final String userServiceBase;
    private final RestTemplate restTemplate = new RestTemplate();

    public RestUserClientAdapter(@Value("${USER_SERVICE_URL:http://user-service:3002}") String userServiceBase) {
        this.userServiceBase = userServiceBase;
    }

    @Override
    public String getUserEmail(String userId) {
        if (userId == null) return null;
        try {
            String url = String.format("%s/users/%s", userServiceBase, userId);
            @SuppressWarnings("unchecked")
            Map<String, Object> user = restTemplate.getForObject(url, Map.class);
            if (user != null && user.get("email") != null) {
                return String.valueOf(user.get("email"));
            }
        } catch (Exception e) {
            // Do not throw from port — treat as unresolved and let caller decide fallback
        }
        return null;
    }
}
