package Ecom.user_service.dto.response;

import lombok.*;

@Data @Builder
public class AddressResponse {
    private Long id;
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private boolean isDefault;
}