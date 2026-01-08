package com.example.budgettracker.config;

import com.example.budgettracker.repository.UserRepository;
import com.example.budgettracker.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MonthlySummaryEmailScheduler {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public MonthlySummaryEmailScheduler(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 9 1 * *") // 9am, first of every month
    public void sendMonthlySummaries() {
        userRepository.findAll().forEach(user -> {
            if (Boolean.TRUE.equals(user.getAllowNotificationEmail())) {
                emailService.sendMonthlySummaryEmail(
                        user.getEmail(),
                        user.getFirstName(),
                        null // you can later pass an HTML report summary here
                );
            }
        });
    }
}
