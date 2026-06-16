package com.example.salonflow.security.oauth;

import com.example.salonflow.entity.enums.OAuthProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public record OAuth2UserInfo(
        OAuthProvider provider,
        String providerUserId,
        String email,
        String name,
        String avatarUrl,
        Boolean emailVerified
) {

    public static OAuth2UserInfo from(
            String registrationId,
            OAuth2User user
    ) {

        OAuthProvider provider =
                OAuthProvider.valueOf(registrationId.toUpperCase());

        return switch (provider) {
            case GOOGLE -> google(user);
            case FACEBOOK -> facebook(user);
        };
    }

    private static OAuth2UserInfo google(OAuth2User user) {

        return new OAuth2UserInfo(
                OAuthProvider.GOOGLE,
                user.getAttribute("sub"),
                user.getAttribute("email"),
                user.getAttribute("name"),
                user.getAttribute("picture"),
                user.getAttribute("email_verified")
        );
    }

    @SuppressWarnings("unchecked")
    private static OAuth2UserInfo facebook(OAuth2User user) {

        String avatarUrl = null;
        Map<String, Object> picture = user.getAttribute("picture");

        if (picture != null) {
            Object data = picture.get("data");

            if (data instanceof Map<?, ?> dataMap) {
                Object url = dataMap.get("url");
                avatarUrl = url == null ? null : url.toString();
            }
        }

        return new OAuth2UserInfo(
                OAuthProvider.FACEBOOK,
                user.getAttribute("id"),
                user.getAttribute("email"),
                user.getAttribute("name"),
                avatarUrl,
                null
        );
    }
}
