package com.eventhub.web;

import com.eventhub.dto.ApiDtos.*;
import com.eventhub.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return auth.register(request); }
    @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }
    @GetMapping("/me") public UserView me(@AuthenticationPrincipal String email) { return UserView.from(auth.requireUser(email)); }
    @PutMapping("/me") public UserView update(@AuthenticationPrincipal String email, @Valid @RequestBody ProfileRequest request) {
        return auth.updateProfile(email, request);
    }
    @PutMapping("/me/password") public Message password(@AuthenticationPrincipal String email, @Valid @RequestBody PasswordRequest request) {
        auth.changePassword(email, request); return new Message("Password changed");
    }
}
