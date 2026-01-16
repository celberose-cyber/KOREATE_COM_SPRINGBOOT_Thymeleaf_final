package org.zerock.com.example.user;

import org.springframework.stereotype.Component;

@Component
public class ConsoleEmailSender implements EmailSender {
    @Override
    public void send(String to, String subject, String body) {
        System.out.println("=== SEND MAIL ===");
        System.out.println("TO: " + to);
        System.out.println("SUBJECT: " + subject);
        System.out.println(body);
        System.out.println("=================");
    }
}