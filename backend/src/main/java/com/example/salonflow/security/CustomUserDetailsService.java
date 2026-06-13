package com.example.salonflow.security;

import com.example.salonflow.entity.User;
import com.example.salonflow.entity.enums.UserStatus;
import com.example.salonflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream; // Thêm import này để hết lỗi biên dịch

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Gom cả ROLE_ và các Permissions lại thành một danh sách Authority phẳng
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    // 1. Tạo authority cho Role (Ví dụ: ROLE_ADMIN)
                    Stream<GrantedAuthority> roleAuth = Stream.of(
                            new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase())
                    );

                    // 2. Tạo authority cho các Permission thuộc Role đó (Ví dụ: READ_PRIVILEGE)
                    Stream<GrantedAuthority> permissionAuth = role.getPermissions().stream()
                            .map(permission -> new SimpleGrantedAuthority(permission.getName().toUpperCase()));

                    // Gộp 2 stream lại
                    return Stream.concat(roleAuth, permissionAuth);
                })
                .toList();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash()) // Đảm bảo thuộc tính này đúng trong entity User của bạn
                .authorities(authorities)
                .disabled(user.getStatus() != UserStatus.ACTIVE) // Nếu trạng thái KHÁC ACTIVE thì sẽ bị disable
                .build();
    }
}