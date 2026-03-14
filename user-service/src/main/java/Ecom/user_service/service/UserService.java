package Ecom.user_service.service;

import Ecom.user_service.dto.request.*;
import Ecom.user_service.dto.response.*;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getUserById(Long id);
    UserResponse updateUser(Long id, RegisterRequest request);
    AddressResponse addAddress(Long userId, AddressRequest request);
    void saveRecipe(Long userId, Long recipeId);
    void removeSavedRecipe(Long userId, Long recipeId);
}