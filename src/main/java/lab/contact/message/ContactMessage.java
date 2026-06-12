package lab.contact.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "messages")
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String message;

    // NULL = ยังไม่อ่าน — เก็บ "เมื่อไหร่" ได้ฟรีแทน boolean
    @Column(name = "read_at")
    private Instant readAt;

    @Generated
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected ContactMessage() {
    }

    public ContactMessage(String name, String email, String message) {
        this.name = name;
        this.email = email;
        this.message = message;
    }

    public void markRead() {
        if (readAt == null) {
            // ตัดเหลือไมโครวินาทีเท่าที่ Postgres เก็บได้ — ค่าใน response แรก
            // จะตรงกับที่อ่านกลับจาก DB ทุกครั้ง (บน Linux นาฬิกาให้ระดับนาโน)
            readAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getMessage() { return message; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
