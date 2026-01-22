package org.zerock.com.example.user;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
//@Profile("prod")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // ⭐ true = HTML

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("HTML 메일 전송 실패", e);
        }
    }

    @Override
    public void send(String to, String subject, String body) {
        // 필요하면 text/plain 용도
        throw new UnsupportedOperationException("text 메일 미사용");
    }
}
