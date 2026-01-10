package com.hotel.pms.service;

import com.hotel.pms.model.entity.Payment;
import com.hotel.pms.model.entity.StayNight;
import com.hotel.pms.model.entity.Booking;
import com.hotel.pms.model.entity.Room;
import com.hotel.pms.model.enums.PaymentMethod;
import com.hotel.pms.model.enums.BookingStatus;
import com.hotel.pms.repository.PaymentRepository;
import com.hotel.pms.repository.StayNightRepository;
import com.hotel.pms.repository.BookingRepository;
import com.hotel.pms.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private StayNightRepository stayNightRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    public Map<String, Object> getRevenueReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Payment> payments = paymentRepository.findByPaymentDateBetween(start, end);

        BigDecimal totalRevenue = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashRevenue = paymentRepository.sumByPaymentDateBetweenAndPaymentMethod(
                start, end, PaymentMethod.CASH);
        if (cashRevenue == null) cashRevenue = BigDecimal.ZERO;

        BigDecimal cardRevenue = paymentRepository.sumByPaymentDateBetweenAndPaymentMethod(
                start, end, PaymentMethod.CARD);
        if (cardRevenue == null) cardRevenue = BigDecimal.ZERO;

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalRevenue", totalRevenue);
        report.put("cashRevenue", cashRevenue);
        report.put("cardRevenue", cardRevenue);
        report.put("paymentCount", payments.size());

        return report;
    }

    public Map<String, Object> getOccupancyReport(LocalDate startDate, LocalDate endDate) {
        List<StayNight> stayNights = stayNightRepository.findByStayDateBetween(startDate, endDate);

        Map<LocalDate, Long> occupancyByDate = new HashMap<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            long count = stayNightRepository.countByStayDate(current);
            occupancyByDate.put(current, count);
            current = current.plusDays(1);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalNights", stayNights.size());
        report.put("occupancyByDate", occupancyByDate);
        report.put("averageOccupancy", occupancyByDate.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0));

        return report;
    }

    public Map<String, Object> getNightsReport(LocalDate startDate, LocalDate endDate) {
        List<StayNight> stayNights = stayNightRepository.findByStayDateBetween(startDate, endDate);

        BigDecimal totalRevenue = stayNights.stream()
                .map(StayNight::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalNights", stayNights.size());
        report.put("totalRevenue", totalRevenue);
        report.put("averagePricePerNight", stayNights.isEmpty() ? BigDecimal.ZERO :
                totalRevenue.divide(BigDecimal.valueOf(stayNights.size()), 2, java.math.RoundingMode.HALF_UP));

        return report;
    }

    public Map<String, Object> getRoomOccupancyCalendar(LocalDate startDate, LocalDate endDate) {
        List<Room> allRooms = roomRepository.findByActiveTrue();
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .filter(b -> !b.getCheckOutDate().isBefore(startDate) && !b.getCheckInDate().isAfter(endDate))
                .collect(Collectors.toList());

        Map<String, Map<String, Object>> calendar = new HashMap<>();
        
        // Initialize calendar with all rooms and dates
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            final LocalDate date = current; // Make final for use in lambda
            String dateKey = date.toString();
            Map<String, Object> dateData = new HashMap<>();
            
            for (Room room : allRooms) {
                final Long roomId = room.getId(); // Make final for use in lambda
                boolean isOccupied = bookings.stream().anyMatch(b -> 
                    b.getRoom().getId().equals(roomId) &&
                    !b.getCheckInDate().isAfter(date) &&
                    !b.getCheckOutDate().isBefore(date.plusDays(1))
                );
                
                Booking booking = bookings.stream()
                    .filter(b -> b.getRoom().getId().equals(roomId) &&
                               !b.getCheckInDate().isAfter(date) &&
                               !b.getCheckOutDate().isBefore(date.plusDays(1)))
                    .findFirst()
                    .orElse(null);
                
                Map<String, Object> roomStatus = new HashMap<>();
                roomStatus.put("occupied", isOccupied);
                roomStatus.put("roomNumber", room.getRoomNumber());
                roomStatus.put("roomType", room.getRoomType().getName());
                if (booking != null) {
                    roomStatus.put("bookingId", booking.getId());
                    roomStatus.put("guestName", booking.getGuest().getFirstName() + " " + booking.getGuest().getLastName());
                    roomStatus.put("status", booking.getStatus().toString());
                }
                
                dateData.put(room.getRoomNumber(), roomStatus);
            }
            
            calendar.put(dateKey, dateData);
            current = current.plusDays(1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("rooms", allRooms.stream().map(r -> Map.of(
            "id", r.getId(),
            "roomNumber", r.getRoomNumber(),
            "roomType", r.getRoomType().getName()
        )).collect(Collectors.toList()));
        result.put("calendar", calendar);
        
        return result;
    }
}

