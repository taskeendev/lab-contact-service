package lab.contact;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final String serviceName;

    // อ่านจาก config แทนฝังชื่อ — ลอกไฟล์ข้าม service แล้วชื่อไม่หลงเหมือนคราวนี้
    public HealthController(@Value("${spring.application.name}") String serviceName) {
        this.serviceName = serviceName;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "up", "service", serviceName);
    }
}
