package com.eventhub.service;

import com.eventhub.dto.ApiDtos.*;
import com.eventhub.model.*;
import com.eventhub.repository.UserRepository;
import com.eventhub.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    public AuthService(UserRepository users, PasswordEncoder encoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users; this.encoder = encoder; this.authenticationManager = authenticationManager; this.jwtService = jwtService;
    }
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw DomainException.conflict("An account with this email already exists");
        User user = new User(request.fullName().trim(), email, encoder.encode(request.password()), Role.USER);
        user.setPhoneNumber(request.phoneNumber());
        user = users.save(user);
        return new AuthResponse(jwtService.createToken(user), UserView.from(user));
    }
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        } catch (AuthenticationException ex) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
        }
        User user = users.findByEmailIgnoreCase(request.email()).orElseThrow(() -> DomainException.notFound("User not found"));
        return new AuthResponse(jwtService.createToken(user), UserView.from(user));
    }
    public User requireUser(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> DomainException.notFound("User not found"));
    }
    @Transactional
    public UserView updateProfile(String email, ProfileRequest request) {
        User user = requireUser(email);
        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(request.phoneNumber());
        return UserView.from(user);
    }
    @Transactional
    public void changePassword(String email, PasswordRequest request) {
        User user = requireUser(email);
        if (!encoder.matches(request.currentPassword(), user.getPasswordHash()))
            throw DomainException.badRequest("Current password is incorrect");
        user.setPasswordHash(encoder.encode(request.newPassword()));
    }
}
