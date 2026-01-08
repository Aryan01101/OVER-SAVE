package com.example.budgettracker.controller;

import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.UserRepository;
import com.example.budgettracker.service.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/notifications")
public class NotificationPreferenceController {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationPreferenceController(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @GetMapping
    public ResponseEntity<Boolean> getPreference(@RequestParam Long userId) {
        Boolean allow = userRepository.getNotificationEmailStatus(userId);
        return ResponseEntity.ok(allow != null && allow);
    }

    @PutMapping
    @Transactional
    public ResponseEntity<Void> updatePreference(@RequestParam Long userId,
                                                 @RequestBody Boolean enabled) {
        userRepository.updateNotificationEmail(userId, enabled);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<Void> sendTestEmail(@RequestParam Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        emailService.sendTestEmail(user.getEmail(), user.getFirstName());
        return ResponseEntity.noContent().build();
    }
}
