package com.saroj.machine_maintenance_portal.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.saroj.machine_maintenance_portal.model.AppUser;
import com.saroj.machine_maintenance_portal.model.Roles;
import com.saroj.machine_maintenance_portal.repository.UserRepository;

/** Creates demo users on first start. Change these passwords for anything beyond a demo. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createIfMissing("admin", "admin123", "System Admin", Roles.ADMIN);
        createIfMissing("manager", "manager123", "Maintenance Manager", Roles.MANAGER);
        createIfMissing("tech1", "tech123", "Technician One", Roles.TECHNICIAN);
        createIfMissing("tech2", "tech123", "Technician Two", Roles.TECHNICIAN);
    }

    private void createIfMissing(String username, String rawPassword, String fullName, String role) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(new AppUser(username, passwordEncoder.encode(rawPassword), fullName, role));
        }
    }
}
