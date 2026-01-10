package com.hotel.pms.controller;

import com.hotel.pms.fiscal.FiscalException;
import com.hotel.pms.model.dto.PaymentDto;
import com.hotel.pms.model.entity.FiscalReceipt;
import com.hotel.pms.model.entity.Payment;
import com.hotel.pms.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Payment> processPayment(@Valid @RequestBody PaymentDto dto) throws FiscalException {
        return ResponseEntity.ok(paymentService.processPayment(dto));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<Payment>> getPaymentsByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentsByBooking(bookingId));
    }

    @PostMapping("/{id}/storno")
    public ResponseEntity<FiscalReceipt> stornoPayment(@PathVariable Long id) throws FiscalException {
        return ResponseEntity.ok(paymentService.stornoPayment(id));
    }

    /**
     * Print Z report (daily closure) - ЗАДЪЛЖИТЕЛЕН всеки ден
     * Затваря дневния период на фискалния принтер
     * Само ADMIN може да принтира Z-отчет
     */
    @PostMapping("/fiscal/z-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.hotel.pms.fiscal.FiscalResult> printZReport() throws FiscalException {
        return ResponseEntity.ok(paymentService.printZReport());
    }

    /**
     * Print X report (intermediate report) - опционален
     * НЕ затваря дневния период, само показва междинна информация
     * Само ADMIN може да принтира X-отчет
     */
    @PostMapping("/fiscal/x-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.hotel.pms.fiscal.FiscalResult> printXReport() throws FiscalException {
        return ResponseEntity.ok(paymentService.printXReport());
    }

    /**
     * Get history of fiscal reports (Z and X reports)
     * Само ADMIN може да вижда историята
     */
    @GetMapping("/fiscal/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.hotel.pms.model.entity.FiscalReport>> getFiscalReportsHistory() {
        return ResponseEntity.ok(paymentService.getFiscalReportsHistory());
    }

    /**
     * Get fiscal reports by type (Z_REPORT or X_REPORT)
     * Само ADMIN може да вижда историята
     */
    @GetMapping("/fiscal/reports/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.hotel.pms.model.entity.FiscalReport>> getFiscalReportsByType(
            @PathVariable String type) {
        com.hotel.pms.model.entity.FiscalReport.ReportType reportType;
        try {
            reportType = com.hotel.pms.model.entity.FiscalReport.ReportType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.hotel.pms.exception.BusinessException("Invalid report type: " + type);
        }
        return ResponseEntity.ok(paymentService.getFiscalReportsByType(reportType));
    }

    /**
     * Generate preview of Z or X report (without printing)
     * Само ADMIN може да генерира preview
     */
    @GetMapping("/fiscal/reports/preview/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.hotel.pms.model.dto.FiscalReportDetailsDto> generateReportPreview(
            @PathVariable String type) {
        com.hotel.pms.model.entity.FiscalReport.ReportType reportType;
        try {
            reportType = com.hotel.pms.model.entity.FiscalReport.ReportType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.hotel.pms.exception.BusinessException("Invalid report type: " + type);
        }
        return ResponseEntity.ok(paymentService.generateReportPreview(reportType));
    }

    /**
     * Get details of a printed report from history
     * Само ADMIN може да вижда детайлите
     */
    @GetMapping("/fiscal/reports/{id}/details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.hotel.pms.model.dto.FiscalReportDetailsDto> getReportDetails(
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getReportDetails(id));
    }
}

