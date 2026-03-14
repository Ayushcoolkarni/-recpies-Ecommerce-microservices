package Ecom.user_service.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private boolean isVerified;
    private LocalDateTime createdAt;
    private List<AddressResponse> addresses;
}