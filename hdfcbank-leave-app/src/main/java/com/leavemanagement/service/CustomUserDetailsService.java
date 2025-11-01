package com.leavemanagement.service;

import com.leavemanagement.model.User;
import com.leavemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String employeeId) throws UsernameNotFoundException {
        Optional<User> userOpt = userRepository.findByEmployeeId(employeeId);
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found with employee ID: " + employeeId);
        }

        User user = userOpt.get();
        
        return new org.springframework.security.core.userdetails.User(
            user.getEmployeeId(),
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority(user.getRole().toString()))
        );
    }
}