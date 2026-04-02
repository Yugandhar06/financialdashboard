package com.finance.dashboard.config;

import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    public DatabaseSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        Optional<User> userOpt = userRepository.findByEmail("admin@finance.com");
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                userRepository.save(user);
                System.out.println("Elevated admin@finance.com to ADMIN role successfully!");
            }
        }
    }
}
