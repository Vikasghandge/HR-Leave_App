package com.leavemanagement.repository;

import com.leavemanagement.model.Leave;
import com.leavemanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {
    List<Leave> findByEmployeeOrderByAppliedDateDesc(User employee);
    List<Leave> findAllByOrderByAppliedDateDesc();
    List<Leave> findByStatus(Leave.LeaveStatus status);
}