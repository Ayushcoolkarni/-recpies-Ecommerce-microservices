package Ecom.user_service.dto.request;

import lombok.Data;

@Data
public class AddressRequest {
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private boolean isDefault;
}