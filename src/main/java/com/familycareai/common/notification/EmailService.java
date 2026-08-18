package com.familycareai.common.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@familycareai.com}")
    private String fromEmail;

    public void sendWelcomeEmail(String toEmail, String firstName, String role) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "noreply@familycareai.com";
        helper.setFrom(sender);
        helper.setTo(toEmail);
        helper.setSubject("Welcome to FamilyCare AI - Account Created Successfully");

        String formattedRole = role != null ? role.replace("ROLE_", "") : "PATIENT";

        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background: #1e293b; border-radius: 16px; padding: 32px; border: 1px solid #334155; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3); }
                        .header { text-align: center; border-bottom: 1px solid #334155; padding-bottom: 20px; margin-bottom: 24px; }
                        .logo { font-size: 24px; font-weight: 800; color: #14b8a6; letter-spacing: -0.5px; }
                        .content { font-size: 14px; line-height: 1.6; color: #cbd5e1; }
                        .badge { display: inline-block; background: #0f766e; color: #99f6e4; padding: 4px 12px; border-radius: 9999px; font-size: 12px; font-weight: 600; margin-top: 4px; }
                        .footer { margin-top: 32px; border-top: 1px solid #334155; padding-top: 16px; text-align: center; font-size: 11px; color: #64748b; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">FamilyCare <span style="color: #2dd4bf;">AI</span></div>
                            <p style="margin: 4px 0 0 0; font-size: 12px; color: #94a3b8;">Enterprise Family Health & Coordinated Care</p>
                        </div>
                        <div class="content">
                            <h2 style="color: #f1f5f9; margin-top: 0;">Welcome, %s!</h2>
                            <p>Thank you for creating an account on the <strong>FamilyCare AI</strong> platform. Your account has been initialized with the following platform role:</p>
                            <p><span class="badge">%s</span></p>
                            <p>You can now access your health portal, monitor real-time vitals, and coordinate family care securely.</p>
                            <div style="background: #090d16; padding: 16px; border-radius: 12px; border-left: 4px solid #14b8a6; margin: 24px 0;">
                                <strong style="color: #f8fafc;">Security Notice:</strong>
                                <p style="margin: 4px 0 0 0; font-size: 12px; color: #94a3b8;">If you did not create this account, please immediately contact our Security Operations Center at <a href="mailto:security@familycareai.com" style="color: #2dd4bf;">security@familycareai.com</a>.</p>
                            </div>
                        </div>
                        <div class="footer">
                            &copy; 2026 FamilyCare AI Inc. All rights reserved. | HIPAA Compliant Enterprise Platform
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(firstName, formattedRole);

        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
        log.info("Successfully sent HTML welcome email to: {}", toEmail);
    }
}
