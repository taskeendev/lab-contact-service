package lab.contact;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Postgres จริงจาก Testcontainers + SMTP จริงจาก GreenMail — จับเมลที่ส่งมาดูได้ทั้งฉบับ
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "jwt.secret=contact-integration-test-secret-must-be-long-42",
            "contact.notify-to=owner@test.local",
            "contact.notify-from=lab@test.local",
            "spring.mail.properties.mail.smtp.auth=false",
            "spring.mail.properties.mail.smtp.starttls.enable=false",
        })
@Testcontainers
class ContactFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.mail.host", () -> "localhost");
        // ถาม setup ไม่ใช่ตัว server — ตอน resolve property server ยังไม่ start
        registry.add("spring.mail.port", ServerSetupTest.SMTP::getPort);
    }

    static final SecretKey KEY = Keys.hmacShaKeyFor(
            "contact-integration-test-secret-must-be-long-42".getBytes(StandardCharsets.UTF_8));

    @Autowired
    TestRestTemplate rest;

    static String token(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(KEY)
                .compact();
    }

    static final String USER = token("alice", "USER");
    static final String ADMIN = token("boss", "ADMIN");

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private int statusOf(String url, HttpMethod method, String token) {
        HttpEntity<?> entity = new HttpEntity<>(null, token != null ? bearer(token) : new HttpHeaders());
        return rest.exchange(url, method, entity, Map.class).getStatusCode().value();
    }

    @Test
    void fullContactFlow() throws Exception {
        // นิรนามส่งได้ + อีเมลแจ้งเตือนต้องไปถึง (รอ async)
        ResponseEntity<Void> submitted = rest.postForEntity("/api/contact",
                Map.of("name", "Visitor", "email", "visitor@example.com",
                        "message", "hello from the integration test"),
                Void.class);
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        MimeMessage mail = greenMail.getReceivedMessages()[0];
        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo("owner@test.local");
        assertThat(mail.getSubject()).contains("Visitor");
        assertThat(GreenMailUtil.getBody(mail)).contains("hello from the integration test");

        // validate ราย field
        ResponseEntity<Map> bad = rest.postForEntity("/api/contact",
                Map.of("name", "", "email", "not-an-email", "message", ""), Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) bad.getBody().get("errors"))
                .containsKeys("name", "email", "message");

        // กล่องขาเข้า: นิรนาม 401 / USER 403 / ADMIN 200 พร้อม total+unread
        assertThat(statusOf("/api/contact", HttpMethod.GET, null)).isEqualTo(401);
        assertThat(statusOf("/api/contact", HttpMethod.GET, USER)).isEqualTo(403);
        ResponseEntity<Map> inbox = rest.exchange("/api/contact", HttpMethod.GET,
                new HttpEntity<>(bearer(ADMIN)), Map.class);
        assertThat(inbox.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) inbox.getBody().get("total")).longValue()).isEqualTo(1);
        assertThat(((Number) inbox.getBody().get("unread")).longValue()).isEqualTo(1);
        List<Map<String, Object>> items = (List<Map<String, Object>>) inbox.getBody().get("items");
        long id = ((Number) items.get(0).get("id")).longValue();
        assertThat(items.get(0).get("readAt")).isNull();

        // mark read: ยิงซ้ำ readAt ไม่ขยับ + unread เหลือ 0
        ResponseEntity<Map> read1 = rest.exchange("/api/contact/" + id + "/read",
                HttpMethod.PATCH, new HttpEntity<>(bearer(ADMIN)), Map.class);
        assertThat(read1.getBody().get("readAt")).isNotNull();
        Thread.sleep(50);
        ResponseEntity<Map> read2 = rest.exchange("/api/contact/" + id + "/read",
                HttpMethod.PATCH, new HttpEntity<>(bearer(ADMIN)), Map.class);
        assertThat(read2.getBody().get("readAt")).isEqualTo(read1.getBody().get("readAt"));
        Map<String, Object> after = rest.exchange("/api/contact", HttpMethod.GET,
                new HttpEntity<>(bearer(ADMIN)), Map.class).getBody();
        assertThat(((Number) after.get("unread")).longValue()).isZero();

        // ลบ: USER 403, ADMIN 204, ไม่มีจริง 404 — mark read ของที่ไม่มีก็ 404
        assertThat(statusOf("/api/contact/" + id, HttpMethod.DELETE, USER)).isEqualTo(403);
        assertThat(statusOf("/api/contact/" + id, HttpMethod.DELETE, ADMIN)).isEqualTo(204);
        assertThat(statusOf("/api/contact/" + id, HttpMethod.DELETE, ADMIN)).isEqualTo(404);
        assertThat(statusOf("/api/contact/99999/read", HttpMethod.PATCH, ADMIN)).isEqualTo(404);
    }
}
