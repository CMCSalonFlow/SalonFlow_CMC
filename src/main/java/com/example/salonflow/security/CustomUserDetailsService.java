package com.example.salonflow.security;

import com.example.salonflow.entity.Permission;
import com.example.salonflow.entity.Role;
import com.example.salonflow.entity.RolePermission;
import com.example.salonflow.entity.User;
import com.example.salonflow.entity.UserRole;
import com.example.salonflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + email
                        ));

        List<GrantedAuthority> authorities =
                user.getUserRoles()
                        .stream()
                        .map(UserRole::getRole)
                        .flatMap(role -> {

                            Stream<GrantedAuthority> roleAuthorities =
                                    Stream.of(
                                            new SimpleGrantedAuthority(
                                                    "ROLE_" + role.getCode().toUpperCase()
                                            )
                                    );

                            Stream<GrantedAuthority> permissionAuthorities =
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

        return new CustomUserPrincipal(
                user,
                authorities
        );
    }
}