package com.example.app.service;

import com.example.app.model.User;
import com.example.app.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Pulls OAuth2 profile info from the provider, upserts a local User record
 * keyed by (provider + providerId), and returns the OAuth2User Spring Security
 * expects for the authenticated session.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attrs = oAuth2User.getAttributes();

        String providerId;
        String email;
        String name;
        String picture;

        // Normalize attribute names across providers
        switch (registrationId) {
            case "google" -> {
                providerId = (String) attrs.get("sub");
                email      = (String) attrs.get("email");
                name       = (String) attrs.get("name");
                picture    = (String) attrs.get("picture");
            }
            case "github" -> {
                providerId = String.valueOf(attrs.get("id"));
                email      = (String) attrs.getOrDefault("email", "");
                name       = (String) attrs.getOrDefault("name", attrs.get("login"));
                picture    = (String) attrs.get("avatar_url");
            }
            default -> {
                providerId = (String) attrs.getOrDefault("sub", attrs.get("id"));
                email      = (String) attrs.getOrDefault("email", "");
                name       = (String) attrs.getOrDefault("name", "");
                picture    = (String) attrs.getOrDefault("picture", "");
            }
        }

        User user = userRepository
                .findByProviderAndProviderId(registrationId, providerId)
                .orElseGet(User::new);

        user.setProvider(registrationId);
        user.setProviderId(providerId);
        user.setEmail(email);
        user.setName(name);
        user.setPicture(picture);
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(Instant.now());
        }
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        return oAuth2User;
    }
}
