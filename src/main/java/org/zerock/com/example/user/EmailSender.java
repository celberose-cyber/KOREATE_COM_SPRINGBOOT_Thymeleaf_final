package org.zerock.com.example.user;

public interface EmailSender {
    void send(String to, String subject, String body);

    void sendHtml(String to, String subject, String html);
        // message.setContent(html, "text/html; charset=UTF-8");
}