package com.leavemanagement.service;

import com.leavemanagement.model.User;
import com.leavemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmployeeId(String employeeId) {
        return userRepository.findByEmployeeId(employeeId);
    }

    public User createEmployee(User user) {
        // Ensure the role is set to EMPLOYEE
        user.setRole(User.Role.EMPLOYEE);
        
        // Encode the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        return userRepository.save(user);
    }

    public User updateUser(User user) {
        // Check if the user exists
        if (userRepository.existsById(user.getId())) {
            // Do not update password if it's empty or null
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                User existingUser = userRepository.findById(user.getId()).orElse(null);
                if (existingUser != null) {
                    user.setPassword(existingUser.getPassword());
                }
            } else {
                // Encode the new password
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            return userRepository.save(user);
        }
        return null;
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean isEmployeeIdAvailable(String employeeId) {
        return !userRepository.existsByEmployeeId(employeeId);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
}