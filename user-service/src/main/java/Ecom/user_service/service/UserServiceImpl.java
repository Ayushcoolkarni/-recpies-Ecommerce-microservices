package Ecom.user_service.service;

import Ecom.user_service.dto.request.*;
import Ecom.user_service.dto.response.*;
import Ecom.user_service.entity.*;
import Ecom.user_service.enums.Role;
import Ecom.user_service.exception.ResourceNotFoundException;
import Ecom.user_service.mapper.*;
import Ecom.user_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository          userRepository;
    private final AddressRepository       addressRepository;
    private final SavedRecipeRepository   savedRecipeRepository;
    private final UserMapper              userMapper;
    private final AddressMapper           addressMapper;
    private final JwtService              jwtService;
    private final PasswordEncoder         passwordEncoder;
    private final AuthenticationManager  authenticationManager;

    // ── AUTH ──────────────────────────────────────────────────────

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.CUSTOMER)
                .isVerified(true)  // auto-verify on registration
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            // Delegates to DaoAuthenticationProvider → CustomUserDetailsService → DB
            // Throws BadCredentialsException if wrong password or user not found
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("Token refreshed for: {}", email);
        return buildAuthResponse(user);
    }

    // ── USER PROFILE ──────────────────────────────────────────────

    @Override
    public java.util.List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return userMapper.toResponse(
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email)));
    }

    @Override
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id)));
    }

    @Override
    public UserResponse updateUser(Long id, RegisterRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        return userMapper.toResponse(userRepository.save(user));
    }

    // ── ADDRESS ───────────────────────────────────────────────────

    @Override
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Address address = addressMapper.toEntity(request);
        address.setUser(user);
        return addressMapper.toResponse(addressRepository.save(address));
    }

    // ── SAVED RECIPES ─────────────────────────────────────────────

    @Override
    @Transactional
    public void saveRecipe(Long userId, Long recipeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        SavedRecipe saved = SavedRecipe.builder()
                .user(user).recipeId(recipeId).build();
        savedRecipeRepository.save(saved);
    }

    @Override
    @Transactional
    public void removeSavedRecipe(Long userId, Long recipeId) {
        savedRecipeRepository.deleteByUserIdAndRecipeId(userId, recipeId);
    }

    // ── private helpers ───────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
