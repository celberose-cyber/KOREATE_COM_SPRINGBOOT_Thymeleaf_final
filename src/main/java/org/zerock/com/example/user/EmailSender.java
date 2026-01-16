package org.zerock.com.example.user;

public interface EmailSender {
    void send(String to, String subject, String body);
}