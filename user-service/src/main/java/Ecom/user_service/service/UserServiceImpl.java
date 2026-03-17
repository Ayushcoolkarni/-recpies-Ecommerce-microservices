package Ecom.user_service.service;

import Ecom.user_service.dto.request.*;
import Ecom.user_service.dto.response.*;
import Ecom.user_service.entity.*;
import Ecom.user_service.enums.Role;
import Ecom.user_service.exception.ResourceNotFoundException;
import Ecom.user_service.mapper.*;
import Ecom.user_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final SavedRecipeRepository savedRecipeRepository;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email already registered");

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .userId(user.getId())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new RuntimeException("Invalid credentials");

        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .tokenType("Bearer")
                .userId(user.getId())
                .role(user.getRole().name())
                .build();
    }


    @Override
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    @Override
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Address address = addressMapper.toEntity(request);
        address.setUser(user);
        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void saveRecipe(Long userId, Long recipeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        SavedRecipe saved = SavedRecipe.builder()
                .user(user).recipeId(recipeId).build();
        savedRecipeRepository.save(saved);
    }

    @Override
    @Transactional
    public void removeSavedRecipe(Long userId, Long recipeId) {
        savedRecipeRepository.deleteByUserIdAndRecipeId(userId, recipeId);
    }

    @Override
    public UserResponse updateUser(Long id, RegisterRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        return userMapper.toResponse(userRepository.save(user));
    }
}