package com.familycareai.identity.listener;

import com.familycareai.common.notification.EmailService;
import com.familycareai.identity.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventListener {

    private final EmailService emailService;

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Processing UserRegisteredEvent asynchronously for user: {} [Thread: {}]",
                event.email(), Thread.currentThread().getName());
        try {
            emailService.sendWelcomeEmail(event.email(), event.firstName(), event.role());
        } catch (Exception ex) {
            log.error("Failed to send welcome email to {}. Error: {}", event.email(), ex.getMessage(), ex);
            // Non-blocking fault tolerance: Exception is caught and logged without affecting database transaction
        }
    }
}
