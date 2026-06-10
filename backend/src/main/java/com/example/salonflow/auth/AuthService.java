package com.example.salonflow.auth;

import com.example.salonflow.dto.AuthResponse;
import com.example.salonflow.dto.RegisterRequest;
import com.example.salonflow.entity.User;
import com.example.salonflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder(12);

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();

        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());

        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        userRepository.save(user);

        String accessToken =
                jwtService.generateAccessToken(user.getEmail());

        String refreshToken =
                jwtService.generateRefreshToken(user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken
        );
    }
}