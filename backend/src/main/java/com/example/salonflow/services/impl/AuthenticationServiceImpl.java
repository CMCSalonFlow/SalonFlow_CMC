package com.example.salonflow.services.impl;

import com.example.salonflow.dto.auth.*;
import com.example.salonflow.dto.common.MessageResponse;
import com.example.salonflow.entity.RefreshToken;
import com.example.salonflow.entity.Role;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.enums.UserStatus;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.exception.InvalidTokenException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.RoleRepository;
import com.example.salonflow.repository.UserRepository;
import com.example.salonflow.services.service.AuthenticationService;
import com.example.salonflow.services.service.EmailService;
import com.example.salonflow.services.service.JwtService;
import com.example.salonflow.services.service.OtpService;
import com.example.salonflow.services.service.RefreshTokenService;
import com.example.salonflow.util.OtpGenerator;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

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

        private final EmailService emailService;
        private final OtpService otpService;
        private final OtpGenerator otpGenerator;

        @Value("${frontend.url}")
        private String frontendUrl;

        @Override
        public RegisterResponse register(
                        RegisterRequest request) {

                validateRegisterRequest(request);

                Role customerRole = roleRepository.findByName(DEFAULT_ROLE)
                                .orElseThrow(() -> 
                                        new ResourceNotFoundException("Role CUSTOMER không tồn tại"));

                User user = User.builder()
                                .username(request.getUsername())
                                .email(request.getEmail())
                                .passwordHash(
                                                passwordEncoder.encode(
                                                                request.getPassword()))
                                .fullName(request.getFullName())
                                .phone(request.getPhone())
                                .status(UserStatus.INACTIVE)
                                .build();

                user.getRoles().add(customerRole);

                user = userRepository.save(user);

                sendVerificationOtp(user.getEmail());

                return RegisterResponse.builder()
                        .email(user.getEmail())
                        .message("OTP sent to your email")
                        .build();
        }

        @Override
        public AuthResponse login(
                        LoginRequest request) {

                try {

                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        request.getEmail(),
                                                        request.getPassword()));

                } catch (AuthenticationException e) {

                        throw new BusinessException("Email hoặc mật khẩu không đúng");
                }

                User user = userRepository
                                .findByEmail(request.getEmail())
                                .orElseThrow(() ->
                                        new ResourceNotFoundException("Người dùng không tồn tại"));

                validateActiveUser(user);

                return buildAuthResponse(user);
        }

        @Override
        public RefreshTokenResponse refreshToken(
                        RefreshTokenRequest request) {

                RefreshToken refreshToken = refreshTokenService.verifyExpiration(
                                request.getRefreshToken());

                User user = refreshToken.getUser();

                validateActiveUser(user);

                UserDetails userDetails = buildUserDetails(user);

                String accessToken = jwtService.generateToken(userDetails);

                return RefreshTokenResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken.getToken())
                                .build();
        }

        @Override
        public void logout(Long userId) {

                User user = userRepository.findById(userId)
                                .orElseThrow(
                                                () -> new RuntimeException(
                                                                "User not found"));

                refreshTokenService.deleteByUser(user);
        }

        private void validateRegisterRequest(
                        RegisterRequest request) {

                if (userRepository.existsByEmail(
                                request.getEmail())) {

                        throw new BusinessException("Email đã tồn tại");
                }

                if (userRepository.existsByUsername(
                                request.getUsername())) {

                        throw new BusinessException("Username đã tồn tại");
                }
        }

        private void validateActiveUser(User user) {

                if (user.getStatus() != UserStatus.ACTIVE) {
                        throw new BusinessException("Tài khoản chưa được kích hoạt");
                }
        }

        private AuthResponse buildAuthResponse(
                        User user) {

                UserDetails userDetails = buildUserDetails(user);

                String accessToken = jwtService.generateToken(userDetails);

                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

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
                                                                .toList())
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
                                                                .map(role -> new SimpleGrantedAuthority(
                                                                                "ROLE_" + role.getName()))
                                                                .toList())
                                .disabled(user.getStatus() != UserStatus.ACTIVE)
                                .build();
        }

        @Override
        public void sendVerificationOtp(
                        String email) {

                String otp = otpGenerator.generate();

                otpService.saveOtp(
                                email,
                                otp);

                emailService.sendVerificationOtp(
                                email,
                                otp);
        }

        @Override
        public MessageResponse verifyEmail(
                        String email,
                        String otp) {

                String savedOtp = otpService.getOtp(email);

                if (savedOtp == null) {
                        throw new InvalidTokenException("OTP đã hết hạn");
                }

                if (!savedOtp.equals(otp)) {
                        throw new InvalidTokenException("OTP không hợp lệ");
                }

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Người dùng không tồn tại"));

                user.setStatus(UserStatus.ACTIVE);

                otpService.deleteOtp(email);

                userRepository.save(user);
                return MessageResponse.builder()
                        .message("Email verified successfully")
                        .build();
        }

        @Override
        public void forgotPassword(
                        String email) {

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() ->
                                        new ResourceNotFoundException("Email không tồn tại trong hệ thống"));

                String token = jwtService.generateResetPasswordToken(
                                user.getEmail());

                String link = frontendUrl
                                + "/reset-password?token="
                                + token;

                emailService.sendResetPasswordEmail(email, link);
        }

        @Override
        public void resetPassword(String token, String newPassword) {

                String email;

                try {
                        email = jwtService.extractEmailFromResetToken(token);
                } catch (Exception e) {
                        throw new InvalidTokenException("Token không hợp lệ hoặc đã hết hạn");
                }

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Người dùng không tồn tại"));

                user.setPasswordHash(
                        passwordEncoder.encode(newPassword));

                userRepository.save(user);
        }

}
