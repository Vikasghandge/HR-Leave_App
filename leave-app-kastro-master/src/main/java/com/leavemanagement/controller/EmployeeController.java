package com.leavemanagement.controller;

import com.leavemanagement.model.Leave;
import com.leavemanagement.model.User;
import com.leavemanagement.service.LeaveService;
import com.leavemanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private UserService userService;

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userOpt = userService.getUserByEmployeeId(auth.getName());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);
            
            // Get employee's leaves
            List<Leave> leaves = leaveService.getLeavesByEmployee(user);
            model.addAttribute("leaves", leaves);
            
            return "employee/dashboard";
        }
        
        return "redirect:/login";
    }

    @GetMapping("/apply-leave")
    public String applyLeaveForm(Model model) {
        // Get current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userOpt = userService.getUserByEmployeeId(auth.getName());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);
            model.addAttribute("leave", new Leave());
            model.addAttribute("leaveReasons", Leave.LeaveReason.values());
            
            return "employee/apply-leave";
        }
        
        return "redirect:/login";
    }

    @PostMapping("/apply-leave")
    public String applyLeave(@ModelAttribute Leave leave, RedirectAttributes redirectAttributes) {
        // Get current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userOpt = userService.getUserByEmployeeId(auth.getName());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            leave.setEmployee(user);
            
            Leave savedLeave = leaveService.applyLeave(leave);
            if (savedLeave != null) {
                redirectAttributes.addFlashAttribute("successMessage", "Leave application submitted successfully");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit leave application");
            }
            
            return "redirect:/employee/dashboard";
        }
        
        return "redirect:/login";
    }

    @GetMapping("/leave-status")
    public String leaveStatus(Model model) {
        // Get current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userOpt = userService.getUserByEmployeeId(auth.getName());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);
            
            // Get employee's leaves
            List<Leave> leaves = leaveService.getLeavesByEmployee(user);
            model.addAttribute("leaves", leaves);
            
            return "employee/leave-status";
        }
        
        return "redirect:/login";
    }
}