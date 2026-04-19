package com.example.app.controller;

import com.example.app.model.User;
import com.example.app.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the currently authenticated user's profile, or 401 if not logged in.
     * This endpoint is permitAll so the frontend can poll it to find out if a
     * session is active.
     */
    @GetMapping("/user")
    public ResponseEntity<?> currentUser(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken authToken)) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }
        OAuth2User principal = authToken.getPrincipal();
        String provider = authToken.getAuthorizedClientRegistrationId();

        String providerId = switch (provider) {
            case "google" -> (String) principal.getAttributes().get("sub");
            case "github" -> String.valueOf(principal.getAttributes().get("id"));
            default       -> (String) principal.getAttributes().getOrDefault("sub",
                             principal.getAttributes().get("id"));
        };

        User user = userRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElse(null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("authenticated", true);
        if (user != null) {
            resp.put("id", user.getId());
            resp.put("email", user.getEmail());
            resp.put("name", user.getName());
            resp.put("picture", user.getPicture());
            resp.put("provider", user.getProvider());
        } else {
            resp.putAll(principal.getAttributes());
        }
        return ResponseEntity.ok(resp);
    }
}
