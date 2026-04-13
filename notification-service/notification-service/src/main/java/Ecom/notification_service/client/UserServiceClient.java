package Ecom.notification_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient restClient;

    /**
     * Fetch user email by userId from user-service.
     * Returns null gracefully if user-service is down or user not found.
     */
    public String getEmailByUserId(Long userId) {
        try {
            Map<?, ?> user = restClient.get()
                    .uri("http://user-service/users/{id}", userId)
                    .retrieve()
                    .body(Map.class);
            if (user != null && user.get("email") != null) {
                return user.get("email").toString();
            }
        } catch (Exception e) {
            log.error("Could not fetch email for userId={}: {}", userId, e.getMessage());
        }
        return null;
    }
}
