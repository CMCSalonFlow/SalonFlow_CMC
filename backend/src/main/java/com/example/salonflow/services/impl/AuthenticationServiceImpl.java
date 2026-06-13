package com.example.salonflow.services.impl;


import com.example.salonflow.dto.auth.*;
import com.example.salonflow.entity.RefreshToken;
import com.example.salonflow.entity.Role;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.enums.UserStatus;
import com.example.salonflow.repository.RoleRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.services.service.AuthenticationService;
import com.example.salonflow.services.service.JwtService;
import com.example.salonflow.services.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationServiceImpl
        implements AuthenticationService {

    private static final String DEFAULT_ROLE = "CUSTOMER";

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public AuthResponse register(
            RegisterRequest request
    ) {

        validateRegisterRequest(request);

        Role customerRole =
                roleRepository.findByName(DEFAULT_ROLE)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Role CUSTOMER not found"
                                ));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .build();

        user.getRoles().add(customerRole);

        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(
            LoginRequest request
    ) {

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

        } catch (AuthenticationException e) {

            throw new RuntimeException(
                    "Email or password invalid"
            );
        }

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new RuntimeException(
                                "User not found"
                        )
                );

        validateActiveUser(user);

        return buildAuthResponse(user);
    }

    @Override
    public RefreshTokenResponse refreshToken(
            RefreshTokenRequest request
    ) {

        RefreshToken refreshToken =
                refreshTokenService.verifyExpiration(
                        request.getRefreshToken()
                );

        User user = refreshToken.getUser();

        validateActiveUser(user);

        UserDetails userDetails = buildUserDetails(user);

        String accessToken =
                jwtService.generateToken(userDetails);

        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Override
    public void logout(Long userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        refreshTokenService.deleteByUser(user);
    }

    private void validateRegisterRequest(
            RegisterRequest request
    ) {

        if(userRepository.existsByEmail(
                request.getEmail()
        )) {

            throw new RuntimeException(
                    "Email already exists"
            );
        }

        if(userRepository.existsByUsername(
                request.getUsername()
        )) {

            throw new RuntimeException(
                    "Username already exists"
            );
        }
    }

    private void validateActiveUser(User user) {

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException(
                    "User account is not active"
            );
        }
    }

    private AuthResponse buildAuthResponse(
            User user
    ) {

        UserDetails userDetails = buildUserDetails(user);

        String accessToken =
                jwtService.generateToken(userDetails);

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .roles(
                        user.getRoles()
                                .stream()
                                .map(Role::getName)
                                .toList()
                )
                .build();
    }

    private UserDetails buildUserDetails(User user) {

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(
                        user.getRoles()
                                .stream()
                                .map(role ->
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role.getName()
                                        )
                                )
                                .toList()
                )
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .build();
    }
}
