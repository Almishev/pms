package com.hotel.pms.service;

import com.hotel.pms.fiscal.DatecsService;
import com.hotel.pms.fiscal.FiscalException;
import com.hotel.pms.fiscal.FiscalResult;
import com.hotel.pms.model.dto.FiscalReportDetailsDto;
import com.hotel.pms.model.dto.PaymentDto;
import com.hotel.pms.model.entity.Booking;
import com.hotel.pms.model.entity.FiscalReceipt;
import com.hotel.pms.model.entity.FiscalReport;
import com.hotel.pms.model.entity.Payment;
import com.hotel.pms.model.entity.StayNight;
import com.hotel.pms.model.entity.User;
import com.hotel.pms.model.enums.FiscalReceiptStatus;
import com.hotel.pms.model.enums.PaymentMethod;
import com.hotel.pms.repository.BookingRepository;
import com.hotel.pms.repository.FiscalReceiptRepository;
import com.hotel.pms.repository.FiscalReportRepository;
import com.hotel.pms.repository.PaymentRepository;
import com.hotel.pms.repository.StayNightRepository;
import com.hotel.pms.repository.UserRepository;
import com.hotel.pms.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FiscalReceiptRepository fiscalReceiptRepository;

    @Autowired
    private FiscalReportRepository fiscalReportRepository;

    @Autowired
    private DatecsService datecsService;

    @Autowired
    private StayNightRepository stayNightRepository;

    @Transactional
    public Payment processPayment(PaymentDto dto) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new BusinessException("Booking not found with id: " + dto.getBookingId()));

        if (dto.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be positive");
        }

        BigDecimal remaining = remainingBalance(booking);
        if (dto.getAmount().compareTo(remaining) > 0) {
            throw new BusinessException("Payment amount exceeds the remaining balance");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(dto.getAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setReversed(false);
        payment.setUser(user);

        payment = paymentRepository.save(payment);

        FiscalResult fiscalResult = datecsService.printReceipt(
                dto.getAmount(),
                dto.getPaymentMethod(),
                "Booking #" + booking.getId()
        );

        if (!fiscalResult.isSuccess()) {
            throw new FiscalException("Failed to print fiscal receipt: " + fiscalResult.getErrorMessage());
        }

        FiscalReceipt fiscalReceipt = new FiscalReceipt();
        fiscalReceipt.setPayment(payment);
        fiscalReceipt.setReceiptNumber(fiscalResult.getReceiptNumber());
        fiscalReceipt.setFiscalDate(fiscalResult.getFiscalDate());
        fiscalReceipt.setTotalAmount(fiscalResult.getTotalAmount());
        fiscalReceipt.setStatus(FiscalReceiptStatus.OK);
        fiscalReceiptRepository.save(fiscalReceipt);

        return payment;
    }

    public List<Payment> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    @Transactional
    public FiscalReceipt stornoPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found with id: " + paymentId));

        if (Boolean.TRUE.equals(payment.getReversed())) {
            throw new BusinessException("Payment is already reversed");
        }

        FiscalReceipt originalReceipt = fiscalReceiptRepository.findByPaymentId(paymentId).stream()
                .filter(receipt -> receipt.getStatus() == FiscalReceiptStatus.OK)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Fiscal receipt not found for payment"));

        payment.setReversed(true);
        paymentRepository.save(payment);

        FiscalResult stornoResult = datecsService.printStorno(originalReceipt.getReceiptNumber());

        if (!stornoResult.isSuccess()) {
            throw new FiscalException("Failed to print storno receipt");
        }

        FiscalReceipt stornoReceipt = new FiscalReceipt();
        stornoReceipt.setPayment(payment);
        stornoReceipt.setReceiptNumber(stornoResult.getReceiptNumber());
        stornoReceipt.setFiscalDate(stornoResult.getFiscalDate());
        stornoReceipt.setTotalAmount(payment.getAmount());
        stornoReceipt.setStatus(FiscalReceiptStatus.STORNO);
        return fiscalReceiptRepository.save(stornoReceipt);
    }

    public BigDecimal remainingBalance(Booking booking) {
        BigDecimal nights = stayNightRepository.findByBookingId(booking.getId()).stream()
                .map(StayNight::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal restaurant = booking.getRestaurantCharge() == null
                ? BigDecimal.ZERO
                : booking.getRestaurantCharge();
        BigDecimal paid = paymentRepository.findByBookingId(booking.getId()).stream()
                .filter(payment -> !Boolean.TRUE.equals(payment.getReversed()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return nights.add(restaurant).subtract(paid);
    }

    /**
     * Print Z report (daily closure) - ЗАДЪЛЖИТЕЛЕН всеки ден
     * Затваря дневния период на фискалния принтер
     */
    @Transactional
    public FiscalResult printZReport() {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("User must be authenticated");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Print Z report on fiscal printer
        FiscalResult result = datecsService.printZReport();
        
        // Save report to database
        com.hotel.pms.model.entity.FiscalReport fiscalReport = new com.hotel.pms.model.entity.FiscalReport();
        fiscalReport.setReportType(com.hotel.pms.model.entity.FiscalReport.ReportType.Z_REPORT);
        fiscalReport.setReportNumber(result.getReceiptNumber());
        fiscalReport.setFiscalDate(result.getFiscalDate());
        fiscalReport.setUser(user);
        fiscalReport.setSuccess(result.isSuccess());
        if (!result.isSuccess()) {
            fiscalReport.setErrorMessage(result.getErrorMessage());
        }
        fiscalReportRepository.save(fiscalReport);
        
        if (!result.isSuccess()) {
            throw new FiscalException("Failed to print Z report: " + result.getErrorMessage());
        }

        return result;
    }

    /**
     * Print X report (intermediate report) - опционален, за междинни проверки
     * НЕ затваря дневния период
     */
    @Transactional
    public FiscalResult printXReport() {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("User must be authenticated");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Print X report on fiscal printer
        FiscalResult result = datecsService.printXReport();
        
        // Save report to database
        com.hotel.pms.model.entity.FiscalReport fiscalReport = new com.hotel.pms.model.entity.FiscalReport();
        fiscalReport.setReportType(com.hotel.pms.model.entity.FiscalReport.ReportType.X_REPORT);
        fiscalReport.setReportNumber(result.getReceiptNumber());
        fiscalReport.setFiscalDate(result.getFiscalDate());
        fiscalReport.setUser(user);
        fiscalReport.setSuccess(result.isSuccess());
        if (!result.isSuccess()) {
            fiscalReport.setErrorMessage(result.getErrorMessage());
        }
        fiscalReportRepository.save(fiscalReport);
        
        if (!result.isSuccess()) {
            throw new FiscalException("Failed to print X report: " + result.getErrorMessage());
        }

        return result;
    }

    /**
     * Get history of fiscal reports (Z and X reports)
     */
    public List<com.hotel.pms.model.entity.FiscalReport> getFiscalReportsHistory() {
        return fiscalReportRepository.findAllByOrderByFiscalDateDesc();
    }

    /**
     * Get fiscal reports by type
     */
    public List<FiscalReport> getFiscalReportsByType(
            FiscalReport.ReportType reportType) {
        return fiscalReportRepository.findByReportTypeOrderByFiscalDateDesc(reportType);
    }

    /**
     * Generate preview of Z or X report (without printing)
     */
    public FiscalReportDetailsDto generateReportPreview(FiscalReport.ReportType reportType) {
        LocalDate today = LocalDate.now();
        LocalDateTime endOfDay = LocalDateTime.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        List<FiscalReport> lastZReports = fiscalReportRepository.findByReportTypeOrderByFiscalDateDesc(
                FiscalReport.ReportType.Z_REPORT);
        if (!lastZReports.isEmpty()) {
            startOfDay = lastZReports.get(0).getFiscalDate();
        }
        
        // Get all payments for the period
        List<Payment> payments = paymentRepository.findByPaymentDateBetween(startOfDay, endOfDay);
        
        // Calculate totals
        BigDecimal totalAmount = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal cashAmount = paymentRepository.sumByPaymentDateBetweenAndPaymentMethod(
                startOfDay, endOfDay, PaymentMethod.CASH);
        if (cashAmount == null) cashAmount = BigDecimal.ZERO;
        
        BigDecimal cardAmount = paymentRepository.sumByPaymentDateBetweenAndPaymentMethod(
                startOfDay, endOfDay, PaymentMethod.CARD);
        if (cardAmount == null) cardAmount = BigDecimal.ZERO;
        
        long cashPayments = payments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.CASH)
                .count();
        
        long cardPayments = payments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.CARD)
                .count();
        
        // Build payment summaries
        List<FiscalReportDetailsDto.PaymentSummary> paymentSummaries = payments.stream()
                .map(payment -> {
                    FiscalReportDetailsDto.PaymentSummary summary = new FiscalReportDetailsDto.PaymentSummary();
                    summary.setPaymentId(payment.getId());
                    summary.setBookingId(payment.getBooking().getId());
                    summary.setGuestName(payment.getBooking().getGuest().getFirstName() + " " + 
                                       payment.getBooking().getGuest().getLastName());
                    summary.setRoomNumber(payment.getBooking().getRoom().getRoomNumber());
                    summary.setAmount(payment.getAmount());
                    summary.setPaymentMethod(payment.getPaymentMethod().toString());
                    summary.setPaymentDate(payment.getPaymentDate());
                    return summary;
                })
                .collect(Collectors.toList());
        
        FiscalReportDetailsDto details = new FiscalReportDetailsDto();
        details.setReportType(reportType.toString());
        details.setReportDate(today);
        details.setFiscalDate(LocalDateTime.now());
        details.setTotalAmount(totalAmount);
        details.setCashAmount(cashAmount);
        details.setCardAmount(cardAmount);
        details.setTotalPayments(payments.size());
        details.setCashPayments((int) cashPayments);
        details.setCardPayments((int) cardPayments);
        details.setPayments(paymentSummaries);
        
        return details;
    }

    /**
     * Get details of a printed report from history
     */
    public FiscalReportDetailsDto getReportDetails(Long reportId) {
        FiscalReport report = fiscalReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("Fiscal report not found with id: " + reportId));
        
        LocalDate reportDate = report.getFiscalDate().toLocalDate();
        LocalDateTime startOfDay = reportDate.atStartOfDay();
        LocalDateTime endOfDay = report.getFiscalDate();
        List<FiscalReport> zReportsBefore = fiscalReportRepository.findByReportTypeOrderByFiscalDateDesc(
                FiscalReport.ReportType.Z_REPORT)
                .stream()
                .filter(z -> z.getFiscalDate().isBefore(report.getFiscalDate()))
                .collect(Collectors.toList());
        if (!zReportsBefore.isEmpty()) {
            startOfDay = zReportsBefore.get(0).getFiscalDate();
        }
        
        // Get all payments for the period
        List<Payment> payments = paymentRepository.findByPaymentDateBetween(startOfDay, endOfDay);
        
        // Calculate totals
        BigDecimal totalAmount = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal cashAmount = paymentRepository.sumByPaymentDateBetweenAndPaymentMethod(
                startOfDay, endOfDay, PaymentMethod.CASH);
        if (cashAmount == null) cashAmount = BigDecimal.ZERO;
        
        BigDecimal cardAmount = paymentRepository.sumByPaymentDateBetweenAndPaymentMethod(
                startOfDay, endOfDay, PaymentMethod.CARD);
        if (cardAmount == null) cardAmount = BigDecimal.ZERO;
        
        long cashPayments = payments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.CASH)
                .count();
        
        long cardPayments = payments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.CARD)
                .count();
        
        // Build payment summaries
        List<FiscalReportDetailsDto.PaymentSummary> paymentSummaries = payments.stream()
                .map(payment -> {
                    FiscalReportDetailsDto.PaymentSummary summary = new FiscalReportDetailsDto.PaymentSummary();
                    summary.setPaymentId(payment.getId());
                    summary.setBookingId(payment.getBooking().getId());
                    summary.setGuestName(payment.getBooking().getGuest().getFirstName() + " " + 
                                       payment.getBooking().getGuest().getLastName());
                    summary.setRoomNumber(payment.getBooking().getRoom().getRoomNumber());
                    summary.setAmount(payment.getAmount());
                    summary.setPaymentMethod(payment.getPaymentMethod().toString());
                    summary.setPaymentDate(payment.getPaymentDate());
                    return summary;
                })
                .collect(Collectors.toList());
        
        FiscalReportDetailsDto details = new FiscalReportDetailsDto();
        details.setReportType(report.getReportType().toString());
        details.setReportNumber(report.getReportNumber());
        details.setReportDate(reportDate);
        details.setFiscalDate(report.getFiscalDate());
        details.setTotalAmount(totalAmount);
        details.setCashAmount(cashAmount);
        details.setCardAmount(cardAmount);
        details.setTotalPayments(payments.size());
        details.setCashPayments((int) cashPayments);
        details.setCardPayments((int) cardPayments);
        details.setPayments(paymentSummaries);
        
        return details;
    }
}

