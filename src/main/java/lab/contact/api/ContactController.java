package lab.contact.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lab.contact.message.ContactMessage;
import lab.contact.message.ContactService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    public record SubmitRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 2000) String message) {}

    public record MessageResponse(
            Long id, String name, String email, String message,
            Instant readAt, Instant createdAt) {

        static MessageResponse from(ContactMessage m) {
            return new MessageResponse(m.getId(), m.getName(), m.getEmail(), m.getMessage(),
                    m.getReadAt(), m.getCreatedAt());
        }
    }

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    // ฝั่งผู้ส่งได้แค่ 201 เปล่า ๆ — เนื้อหากล่องขาเข้าเป็นเรื่องของแอดมิน
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void submit(@Valid @RequestBody SubmitRequest request) {
        contactService.submit(request.name(), request.email(), request.message());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> inbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = contactService.inbox(page, size);
        List<MessageResponse> items = result.map(MessageResponse::from).getContent();
        return Map.of(
                "items", items,
                "total", result.getTotalElements(),
                "unread", contactService.unreadCount());
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public MessageResponse markRead(@PathVariable Long id) {
        return MessageResponse.from(contactService.markRead(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        contactService.delete(id);
    }
}
