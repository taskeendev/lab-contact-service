package lab.contact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ContactServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContactServiceApplication.class, args);
    }
}
