package com.hotel.pms.controller;

import com.hotel.pms.model.dto.CreateBookingDto;
import com.hotel.pms.model.dto.MoveBookingDto;
import com.hotel.pms.model.dto.OpenFolioDto;
import com.hotel.pms.model.entity.Booking;
import com.hotel.pms.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {
    @Autowired
    private BookingService bookingService;

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/open-folios")
    public ResponseEntity<List<OpenFolioDto>> getOpenFolios() {
        return ResponseEntity.ok(bookingService.getOpenFolios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody CreateBookingDto dto) {
        return ResponseEntity.ok(bookingService.createBooking(dto));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<Booking> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.checkIn(id));
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<Booking> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.checkOut(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }

    @PutMapping("/{id}/stay")
    public ResponseEntity<Booking> moveBooking(@PathVariable Long id, @Valid @RequestBody MoveBookingDto dto) {
        return ResponseEntity.ok(bookingService.moveBooking(id, dto));
    }

    @PutMapping("/{id}/restaurant-charge")
    public ResponseEntity<Booking> updateRestaurantCharge(
            @PathVariable Long id,
            @RequestParam BigDecimal restaurantCharge) {
        return ResponseEntity.ok(bookingService.updateRestaurantCharge(id, restaurantCharge));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<Booking>> getBookingsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(bookingService.getBookingsByDateRange(startDate, endDate));
    }
}

