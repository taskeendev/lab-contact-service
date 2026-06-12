package lab.contact.message;

import jakarta.transaction.Transactional;
import lab.contact.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ContactService {

    private final ContactMessageRepository messages;

    public ContactService(ContactMessageRepository messages) {
        this.messages = messages;
    }

    public ContactMessage submit(String name, String email, String message) {
        return messages.save(new ContactMessage(name, email, message));
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
