package com.leavemanagement.config;

import com.leavemanagement.model.User;
import com.leavemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialDataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create default HR admin if not exists
        if (!userRepository.existsByEmployeeId("HR001")) {
            User hrAdmin = new User();
            hrAdmin.setEmployeeId("HR001");
            hrAdmin.setPassword(passwordEncoder.encode("admin123"));
            hrAdmin.setName("HR Admin");
            hrAdmin.setEmail("hr@company.com");
            hrAdmin.setRole(User.Role.HR);
            userRepository.save(hrAdmin);
        }

        // Create a default employee account if not exists
        if (!userRepository.existsByEmployeeId("EMP001")) {
            User employee = new User();
            employee.setEmployeeId("EMP001");
            employee.setPassword(passwordEncoder.encode("emp123"));
            employee.setName("John Doe");
            employee.setEmail("john@company.com");
            employee.setRole(User.Role.EMPLOYEE);
            userRepository.save(employee);
        }
    }
}