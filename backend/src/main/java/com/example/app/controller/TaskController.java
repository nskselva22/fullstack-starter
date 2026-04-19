package com.example.app.controller;

import com.example.app.model.Task;
import com.example.app.model.User;
import com.example.app.repository.TaskRepository;
import com.example.app.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskController(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    private User requireUser(OAuth2User principal, OAuth2AuthenticationToken token) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String provider = token.getAuthorizedClientRegistrationId();
        String providerId = switch (provider) {
            case "google" -> (String) principal.getAttributes().get("sub");
            case "github" -> String.valueOf(principal.getAttributes().get("id"));
            default       -> (String) principal.getAttributes().getOrDefault("sub",
                             principal.getAttributes().get("id"));
        };
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @GetMapping
    public List<Task> list(@AuthenticationPrincipal OAuth2User principal,
                           OAuth2AuthenticationToken token) {
        User user = requireUser(principal, token);
        return taskRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());
    }

    @PostMapping
    public Task create(@Valid @RequestBody Task task,
                       @AuthenticationPrincipal OAuth2User principal,
                       OAuth2AuthenticationToken token) {
        User user = requireUser(principal, token);
        task.setId(null);
        task.setOwnerId(user.getId());
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        return taskRepository.save(task);
    }

    @PutMapping("/{id}")
    public Task update(@PathVariable String id,
                       @Valid @RequestBody Task update,
                       @AuthenticationPrincipal OAuth2User principal,
                       OAuth2AuthenticationToken token) {
        User user = requireUser(principal, token);
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!existing.getOwnerId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        existing.setTitle(update.getTitle());
        existing.setDescription(update.getDescription());
        existing.setDone(update.isDone());
        existing.setUpdatedAt(Instant.now());
        return taskRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @AuthenticationPrincipal OAuth2User principal,
                                       OAuth2AuthenticationToken token) {
        User user = requireUser(principal, token);
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!existing.getOwnerId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        taskRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }
}
