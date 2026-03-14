package Ecom.user_service.service;
import Ecom.user_service.entity.User;

public interface JwtService {
    String generateToken(User user);
    String generateRefreshToken(User user);
}
