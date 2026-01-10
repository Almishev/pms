package com.hotel.pms.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fiscal_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiscalReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 10)
    private ReportType reportType;

    @Column(name = "report_number", length = 50)
    private String reportNumber;

    @Column(name = "fiscal_date", nullable = false)
    private LocalDateTime fiscalDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "success", nullable = false)
    private Boolean success = true;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    public enum ReportType {
        Z_REPORT,
        X_REPORT
    }
}

