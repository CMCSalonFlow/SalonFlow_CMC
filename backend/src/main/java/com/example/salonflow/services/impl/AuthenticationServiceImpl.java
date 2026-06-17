package com.example.salonflow.services.impl;


import com.example.salonflow.dto.auth.*;
import com.example.salonflow.entity.OAuthAccount;
import com.example.salonflow.entity.RefreshToken;
import com.example.salonflow.entity.Role;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.enums.UserStatus;
import com.example.salonflow.repository.OAuthAccountRepository;
import com.example.salonflow.repository.RoleRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.security.oauth.OAuth2UserInfo;
import com.example.salonflow.services.service.AuthenticationService;
import com.example.salonflow.services.service.JwtService;
import com.example.salonflow.services.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.salonflow.entity.Permission;
import com.example.salonflow.entity.RolePermission;
import com.example.salonflow.entity.UserRole;
import com.example.salonflow.entity.UserRoleId;
import com.example.salonflow.repository.UserRoleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationServiceImpl
        implements AuthenticationService {

    private static final String DEFAULT_ROLE = "CUSTOMER";

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
        private void assignRole(User user, Role role) {

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(user.getId(), role.getId()))
                .user(user)
                .role(role)
                .assignedAt(LocalDateTime.now())
                .build();

        userRoleRepository.save(userRole);
        }
        @Override
        public AuthResponse register(
                RegisterRequest request
        ) {

        validateRegisterRequest(request);

        Role customerRole =
                roleRepository.findByCode(DEFAULT_ROLE)
                        .orElseThrow(() -> new RuntimeException("Role CUSTOMER not found"));

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

        user = userRepository.save(user);

        assignRole(
                user,
                customerRole
        );

        return buildAuthResponse(
                userRepository
                        .findByEmailWithRoles(user.getEmail())
                        .orElseThrow()
        );
        }

    @Override
    public AuthResponse loginWithOAuth2(
            String registrationId,
            OAuth2User oauth2User
    ) {

        OAuth2UserInfo userInfo =
                OAuth2UserInfo.from(registrationId, oauth2User);

        validateOAuth2UserInfo(userInfo);

        User user = oauthAccountRepository
                .findByProviderAndProviderUserId(
                        userInfo.provider(),
                        userInfo.providerUserId()
                )
                .map(OAuthAccount::getUser)
                .orElseGet(() -> createOrLinkOAuthAccount(userInfo));

        validateActiveUser(user);

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
                .findByEmailWithRoles(request.getEmail())
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

    private User createOrLinkOAuthAccount(
            OAuth2UserInfo userInfo
    ) {

        User user = userRepository
                .findByEmail(userInfo.email())
                .orElseGet(() -> createOAuthUser(userInfo));

        OAuthAccount oauthAccount = OAuthAccount.builder()
                .provider(userInfo.provider())
                .providerUserId(userInfo.providerUserId())
                .email(userInfo.email())
                .emailVerified(userInfo.emailVerified())
                .user(user)
                .build();

        oauthAccountRepository.save(oauthAccount);

        updateUserProfileFromOAuth(user, userInfo);

        return userRepository.save(user);
    }

    private User createOAuthUser(
                OAuth2UserInfo userInfo
        ) {

                Role customerRole =
                        roleRepository.findByCode(DEFAULT_ROLE)
                                .orElseThrow(() -> new RuntimeException("Role CUSTOMER not found"));

        User user = User.builder()
                .username(generateUniqueUsername(userInfo))
                .email(userInfo.email())
                .passwordHash(
                        passwordEncoder.encode(
                                UUID.randomUUID().toString()
                        )
                )
                .fullName(userInfo.name())
                .avatarUrl(userInfo.avatarUrl())
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        assignRole(
                user,
                customerRole
        );

        return userRepository
                .findByEmailWithRoles(user.getEmail())
                .orElseThrow();
        }

    private void updateUserProfileFromOAuth(
            User user,
            OAuth2UserInfo userInfo
    ) {

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(userInfo.name());
        }

        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(userInfo.avatarUrl());
        }
    }

    private void validateOAuth2UserInfo(
            OAuth2UserInfo userInfo
    ) {

        if (userInfo.providerUserId() == null
                || userInfo.providerUserId().isBlank()) {
            throw new RuntimeException(
                    "OAuth2 provider user id is missing"
            );
        }

        if (userInfo.email() == null
                || userInfo.email().isBlank()) {
            throw new RuntimeException(
                    "OAuth2 provider email is missing"
            );
        }

        if (userInfo.provider()
                == com.example.salonflow.entity.enums.OAuthProvider.GOOGLE
                && !Boolean.TRUE.equals(userInfo.emailVerified())) {
            throw new RuntimeException(
                    "Google email is not verified"
            );
        }
    }

    private String generateUniqueUsername(
            OAuth2UserInfo userInfo
    ) {

        String emailPrefix =
                userInfo.email().split("@")[0]
                        .replaceAll("[^A-Za-z0-9_]", "_");

        String baseUsername =
                emailPrefix.isBlank()
                        ? userInfo.provider().name().toLowerCase()
                        : emailPrefix;

        String username = baseUsername;
        int suffix = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "_" + suffix;
            suffix++;
        }

        return username;
    }

        private AuthResponse buildAuthResponse(
                User user
        ) {

        UserDetails userDetails =
                buildUserDetails(user);

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
                        user.getUserRoles()
                                .stream()
                                .map(UserRole::getRole)
                                .map(Role::getCode)
                                .toList()
                )
                .build();
        }
        
    private UserDetails buildUserDetails(
        User user
) {

    List<SimpleGrantedAuthority> authorities =
            user.getUserRoles()
                    .stream()
                    .map(UserRole::getRole)
                    .flatMap(role -> {

                        Stream<SimpleGrantedAuthority> roleAuthorities =
                                Stream.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role.getCode()
                                        )
                                );

                        Stream<SimpleGrantedAuthority> permissionAuthorities =
                                role.getRolePermissions()
                                        .stream()
                                        .map(RolePermission::getPermission)
                                        .map(Permission::getCode)
                                        .map(String::toUpperCase)
                                        .map(SimpleGrantedAuthority::new);

                        return Stream.concat(
                                roleAuthorities,
                                permissionAuthorities
                        );
                    })
                    .distinct()
                    .toList();

    return org.springframework.security.core.userdetails.User
            .builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(authorities)
            .disabled(
                    user.getStatus()
                            != UserStatus.ACTIVE
            )
            .build();
}
}
