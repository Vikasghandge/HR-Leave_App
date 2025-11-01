package com.leavemanagement.service;

import com.leavemanagement.model.Leave;
import com.leavemanagement.model.User;
import com.leavemanagement.repository.LeaveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    public List<Leave> getAllLeaves() {
        return leaveRepository.findAllByOrderByAppliedDateDesc();
    }

    public List<Leave> getLeavesByEmployee(User employee) {
        return leaveRepository.findByEmployeeOrderByAppliedDateDesc(employee);
    }

    public List<Leave> getLeavesByStatus(Leave.LeaveStatus status) {
        return leaveRepository.findByStatus(status);
    }

    public Optional<Leave> getLeaveById(Long id) {
        return leaveRepository.findById(id);
    }

    public Leave applyLeave(Leave leave) {
        // Set initial status to PENDING
        leave.setStatus(Leave.LeaveStatus.PENDING);
        return leaveRepository.save(leave);
    }

    public Leave updateLeaveStatus(Long leaveId, Leave.LeaveStatus status, String remarks) {
        Optional<Leave> leaveOpt = leaveRepository.findById(leaveId);
        if (leaveOpt.isPresent()) {
            Leave leave = leaveOpt.get();
            leave.setStatus(status);
            leave.setRemarks(remarks);
            return leaveRepository.save(leave);
        }
        return null;
    }

    public boolean deleteLeave(Long id) {
        if (leaveRepository.existsById(id)) {
            leaveRepository.deleteById(id);
            return true;
        }
        return false;
    }
}