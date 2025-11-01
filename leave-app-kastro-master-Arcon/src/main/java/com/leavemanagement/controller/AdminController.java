package com.leavemanagement.controller;

import com.leavemanagement.model.Leave;
import com.leavemanagement.model.User;
import com.leavemanagement.service.LeaveService;
import com.leavemanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Leave> pendingLeaves = leaveService.getLeavesByStatus(Leave.LeaveStatus.PENDING);
        model.addAttribute("pendingLeaves", pendingLeaves);
        
        List<User> employees = userService.getAllUsers();
        model.addAttribute("employees", employees);
        
        return "admin/dashboard";
    }

    @GetMapping("/create-employee")
    public String createEmployeeForm(Model model) {
        model.addAttribute("user", new User());
        return "admin/create-employee";
    }

    @PostMapping("/create-employee")
    public String createEmployee(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        // Check if employee ID already exists
        if (!userService.isEmployeeIdAvailable(user.getEmployeeId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Employee ID already exists");
            return "redirect:/admin/create-employee";
        }
        
        // Check if email already exists
        if (!userService.isEmailAvailable(user.getEmail())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email already exists");
            return "redirect:/admin/create-employee";
        }
        
        User savedUser = userService.createEmployee(user);
        if (savedUser != null) {
            redirectAttributes.addFlashAttribute("successMessage", "Employee created successfully");
            return "redirect:/admin/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create employee");
            return "redirect:/admin/create-employee";
        }
    }

    @GetMapping("/manage-leaves")
    public String manageLeaves(Model model) {
        List<Leave> leaves = leaveService.getAllLeaves();
        model.addAttribute("leaves", leaves);
        
        return "admin/manage-leaves";
    }

    @GetMapping("/leave/{id}")
    public String viewLeave(@PathVariable Long id, Model model) {
        Optional<Leave> leaveOpt = leaveService.getLeaveById(id);
        if (leaveOpt.isPresent()) {
            Leave leave = leaveOpt.get();
            model.addAttribute("leave", leave);
            
            return "admin/leave-details";
        }
        
        return "redirect:/admin/manage-leaves";
    }

    @PostMapping("/leave/{id}/status")
    public String updateLeaveStatus(
            @PathVariable Long id,
            @RequestParam Leave.LeaveStatus status,
            @RequestParam(required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        
        Leave updatedLeave = leaveService.updateLeaveStatus(id, status, remarks);
        if (updatedLeave != null) {
            redirectAttributes.addFlashAttribute("successMessage", "Leave status updated successfully");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update leave status");
        }
        
        return "redirect:/admin/manage-leaves";
    }

    @GetMapping("/employees")
    public String listEmployees(Model model) {
        List<User> employees = userService.getAllUsers();
        model.addAttribute("employees", employees);
        
        return "admin/employees";
    }

    @GetMapping("/employee/{id}")
    public String viewEmployee(@PathVariable Long id, Model model) {
        Optional<User> userOpt = userService.getUserById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);
            
            List<Leave> leaves = leaveService.getLeavesByEmployee(user);
            model.addAttribute("leaves", leaves);
            
            return "admin/employee-details";
        }
        
        return "redirect:/admin/employees";
    }
}