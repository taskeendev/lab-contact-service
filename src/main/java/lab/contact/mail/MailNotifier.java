package lab.contact.mail;

import lab.contact.message.ContactMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class MailNotifier {

    private static final Logger log = LoggerFactory.getLogger(MailNotifier.class);

    private final JavaMailSender sender;
    private final String to;
    private final String from;

    public MailNotifier(
            JavaMailSender sender,
            @Value("${contact.notify-to}") String to,
            @Value("${contact.notify-from}") String from) {
        this.sender = sender;
        this.to = to;
        this.from = from;
    }

    // @Async = ฟอร์มตอบ 201 ทันที ไม่รอ SMTP — เมลล่มแค่ log ข้อความยังอยู่ใน DB ครบ
    @Async
    public void notifyNewMessage(ContactMessage message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(to);
            mail.setFrom(from);
            mail.setSubject("[feature-lab] new contact message from " + message.getName());
            mail.setText("""
                    From: %s <%s>

                    %s

                    — message #%d, stored in the inbox""".formatted(
                    message.getName(), message.getEmail(), message.getMessage(), message.getId()));
            sender.send(mail);
            log.info("notification mail sent for message {}", message.getId());
        } catch (Exception e) {
            log.error("notification mail failed for message {} — message is safe in db",
                    message.getId(), e);
        }
    }
}
