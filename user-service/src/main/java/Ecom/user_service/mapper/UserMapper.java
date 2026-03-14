package Ecom.user_service.mapper;

import Ecom.user_service.dto.response.UserResponse;
import Ecom.user_service.entity.User;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .isVerified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .addresses(user.getAddresses() != null
                        ? user.getAddresses().stream()
                        .map(a -> new AddressMapper().toResponse(a))
                        .collect(Collectors.toList())
                        : null)
                .build();
    }
}