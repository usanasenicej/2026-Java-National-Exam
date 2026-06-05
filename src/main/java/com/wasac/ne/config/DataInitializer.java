package com.wasac.ne.config;

import com.wasac.ne.entity.User;
import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.UserRole;
import com.wasac.ne.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@wasac.rw")) {
            User admin = User.builder()
                    .fullNames("System Administrator")
                    .email("admin@wasac.rw")
                    .phoneNumber("+250788000001")
                    .password(passwordEncoder.encode("Admin@123"))
                    .status(Status.ACTIVE)
                    .roles(Set.of(UserRole.ROLE_ADMIN))
                    .emailVerified(true)
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created: admin@wasac.rw / Admin@123");
        }
    }
}
