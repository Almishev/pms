package com.hotel.pms.controller;

import com.hotel.pms.fiscal.FiscalException;
import com.hotel.pms.model.dto.PaymentDto;
import com.hotel.pms.model.entity.FiscalReceipt;
import com.hotel.pms.model.entity.Payment;
import com.hotel.pms.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
}

