package com.hotel.pms.controller;

import com.hotel.pms.model.entity.StayNight;
import com.hotel.pms.service.StayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stays")
@CrossOrigin(origins = "*")
public class StayController {
    @Autowired
    private StayService stayService;

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<StayNight>> getStayNightsByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(stayService.getStayNightsByBooking(bookingId));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<StayNight>> getStayNightsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(stayService.getStayNightsByDateRange(startDate, endDate));
    }

    @GetMapping("/occupancy/{date}")
    public ResponseEntity<Map<String, Object>> getOccupancy(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        long count = stayService.getOccupancyCount(date);
        return ResponseEntity.ok(Map.of("date", date, "occupancy", count));
    }

    @PutMapping("/booking/{bookingId}/price")
    public ResponseEntity<Map<String, String>> updateStayNightsPrice(
            @PathVariable Long bookingId,
            @RequestParam java.math.BigDecimal pricePerNight) {
        stayService.updateStayNightsPrice(bookingId, pricePerNight);
        return ResponseEntity.ok(Map.of("message", "Stay nights price updated successfully"));
    }
}

