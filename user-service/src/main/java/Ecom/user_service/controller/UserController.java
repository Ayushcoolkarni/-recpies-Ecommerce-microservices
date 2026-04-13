package Ecom.user_service.controller;

import Ecom.user_service.dto.request.*;
import Ecom.user_service.dto.response.*;
import Ecom.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /users — admin: list all users */
    @GetMapping
    public ResponseEntity<java.util.List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /** GET /users/email/{email} — internal: fetch user email (used by notification-service) */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PostMapping("/{id}/addresses")
    public ResponseEntity<AddressResponse> addAddress(@PathVariable Long id,
                                                      @RequestBody AddressRequest request) {
        return ResponseEntity.ok(userService.addAddress(id, request));
    }

    @PostMapping("/{id}/saved-recipes/{recipeId}")
    public ResponseEntity<Void> saveRecipe(@PathVariable Long id,
                                           @PathVariable Long recipeId) {
        userService.saveRecipe(id, recipeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/saved-recipes/{recipeId}")
    public ResponseEntity<Void> removeSavedRecipe(@PathVariable Long id,
                                                  @PathVariable Long recipeId) {
        userService.removeSavedRecipe(id, recipeId);
        return ResponseEntity.noContent().build();
    }

}