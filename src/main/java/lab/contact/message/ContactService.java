package lab.contact.message;

import jakarta.transaction.Transactional;
import lab.contact.error.NotFoundException;
import lab.contact.mail.MailNotifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ContactService {

    private final ContactMessageRepository messages;
    private final MailNotifier notifier;

    public ContactService(ContactMessageRepository messages, MailNotifier notifier) {
        this.messages = messages;
        this.notifier = notifier;
    }

    public ContactMessage submit(String name, String email, String message) {
        ContactMessage saved = messages.save(new ContactMessage(name, email, message));
        notifier.notifyNewMessage(saved); // async — ไม่หน่วงคำตอบของฟอร์ม
        return saved;
    }

    public Page<ContactMessage> inbox(int page, int size) {
        int capped = Math.min(Math.max(size, 1), 50);
        return messages.findAllByOrderByCreatedAtDescIdDesc(
                PageRequest.of(Math.max(page, 0), capped));
    }

    public long unreadCount() {
        return messages.countByReadAtIsNull();
    }

    @Transactional
    public ContactMessage markRead(Long id) {
        ContactMessage message = messages.findById(id)
                .orElseThrow(() -> new NotFoundException("message not found"));
        message.markRead();
        return message;
    }

    public void delete(Long id) {
        if (!messages.existsById(id)) {
            throw new NotFoundException("message not found");
        }
        messages.deleteById(id);
    }
}
