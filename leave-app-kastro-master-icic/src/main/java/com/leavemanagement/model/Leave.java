package com.leavemanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "leaves")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(nullable = false)
    private LocalDate fromDate;

    @Column(nullable = false)
    private LocalDate toDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LeaveReason reason;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column
    private String remarks;

    @Column(nullable = false)
    private LocalDate appliedDate = LocalDate.now();

    public enum LeaveReason {
        SICK_LEAVE, CASUAL_LEAVE, ANNUAL_LEAVE, MATERNITY_LEAVE, PATERNITY_LEAVE, OTHER
    }

    public enum LeaveStatus {
        PENDING, APPROVED, REJECTED
    }
}