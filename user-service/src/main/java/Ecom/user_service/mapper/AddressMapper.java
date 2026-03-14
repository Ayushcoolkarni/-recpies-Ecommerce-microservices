package Ecom.user_service.mapper;

import Ecom.user_service.dto.request.AddressRequest;
import Ecom.user_service.dto.response.AddressResponse;
import Ecom.user_service.entity.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .country(address.getCountry())
                .isDefault(address.isDefault())
                .build();
    }

    public Address toEntity(AddressRequest request) {
        return Address.builder()
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .country(request.getCountry())
                .isDefault(request.isDefault())
                .build();
    }
}