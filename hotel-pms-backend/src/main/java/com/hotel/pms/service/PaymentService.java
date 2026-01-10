package com.hotel.pms.service;

import com.hotel.pms.fiscal.DatecsService;
import com.hotel.pms.fiscal.FiscalException;
import com.hotel.pms.fiscal.FiscalResult;
import com.hotel.pms.model.dto.PaymentDto;
import com.hotel.pms.model.entity.Booking;
import com.hotel.pms.model.entity.FiscalReceipt;
import com.hotel.pms.model.entity.Payment;
import com.hotel.pms.model.entity.User;
import com.hotel.pms.model.enums.FiscalReceiptStatus;
import com.hotel.pms.repository.BookingRepository;
import com.hotel.pms.repository.FiscalReceiptRepository;
import com.hotel.pms.repository.PaymentRepository;
import com.hotel.pms.repository.UserRepository;
import com.hotel.pms.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private DatecsService datecsService;

    @Transactional
    public Payment processPayment(PaymentDto dto) throws FiscalException {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new BusinessException("Booking not found with id: " + dto.getBookingId()));

        if (dto.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be positive");
        }

        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Create payment
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(dto.getAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setUser(user);

        payment = paymentRepository.save(payment);

        // Print fiscal receipt
        try {
            FiscalResult fiscalResult = datecsService.printReceipt(
                    dto.getAmount(),
                    dto.getPaymentMethod(),
                    "Booking #" + booking.getId()
            );

            if (fiscalResult.isSuccess()) {
                FiscalReceipt fiscalReceipt = new FiscalReceipt();
                fiscalReceipt.setPayment(payment);
                fiscalReceipt.setReceiptNumber(fiscalResult.getReceiptNumber());
                fiscalReceipt.setFiscalDate(fiscalResult.getFiscalDate());
                fiscalReceipt.setTotalAmount(fiscalResult.getTotalAmount());
                fiscalReceipt.setStatus(FiscalReceiptStatus.OK);
                fiscalReceiptRepository.save(fiscalReceipt);
            } else {
                FiscalReceipt fiscalReceipt = new FiscalReceipt();
                fiscalReceipt.setPayment(payment);
                fiscalReceipt.setStatus(FiscalReceiptStatus.ERROR);
                fiscalReceiptRepository.save(fiscalReceipt);
                throw new FiscalException("Failed to print fiscal receipt: " + fiscalResult.getErrorMessage());
            }
        } catch (FiscalException e) {
            // Log error but don't fail the payment
            FiscalReceipt fiscalReceipt = new FiscalReceipt();
            fiscalReceipt.setPayment(payment);
            fiscalReceipt.setStatus(FiscalReceiptStatus.ERROR);
            fiscalReceiptRepository.save(fiscalReceipt);
            throw e;
        }

        return payment;
    }

    public List<Payment> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    @Transactional
    public FiscalReceipt stornoPayment(Long paymentId) throws FiscalException {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found with id: " + paymentId));

        FiscalReceipt originalReceipt = fiscalReceiptRepository.findByPaymentId(paymentId);
        if (originalReceipt == null) {
            throw new BusinessException("Fiscal receipt not found for payment");
        }

        // Print storno receipt
        FiscalResult stornoResult = datecsService.printStorno(originalReceipt.getReceiptNumber());

        if (stornoResult.isSuccess()) {
            FiscalReceipt stornoReceipt = new FiscalReceipt();
            stornoReceipt.setPayment(payment);
            stornoReceipt.setReceiptNumber(stornoResult.getReceiptNumber());
            stornoReceipt.setFiscalDate(stornoResult.getFiscalDate());
            stornoReceipt.setTotalAmount(stornoResult.getTotalAmount());
            stornoReceipt.setStatus(FiscalReceiptStatus.STORNO);
            return fiscalReceiptRepository.save(stornoReceipt);
        } else {
            throw new FiscalException("Failed to print storno receipt");
        }
    }
}

