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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
        report.put("payments", payments.stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate))
                .map(payment -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("paymentDate", payment.getPaymentDate());
                    row.put("guestName", payment.getBooking().getGuest().getFirstName() + " "
                            + payment.getBooking().getGuest().getLastName());
                    row.put("roomNumber", payment.getBooking().getRoom().getRoomNumber());
                    row.put("paymentMethod", payment.getPaymentMethod().toString());
                    row.put("amount", payment.getAmount());
                    return row;
                })
                .collect(Collectors.toList()));

        return report;
    }

    public Map<String, Object> getOccupancyReport(LocalDate startDate, LocalDate endDate) {
        List<StayNight> stayNights = stayNightRepository.findByStayDateBetween(startDate, endDate);
        List<Room> rooms = roomRepository.findByActiveTrue();
        int capacity = rooms.size();
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        List<Map<String, Object>> byDate = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            long occupied = stayNightRepository.countByStayDate(current);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", current);
            row.put("occupied", occupied);
            row.put("capacity", capacity);
            row.put("percent", percent(occupied, capacity));
            byDate.add(row);
            current = current.plusDays(1);
        }

        Map<String, Integer> roomsByType = new TreeMap<>();
        for (Room room : rooms) {
            roomsByType.merge(room.getRoomType().getName(), 1, Integer::sum);
        }
        Map<String, Long> nightsByType = new HashMap<>();
        for (StayNight night : stayNights) {
            nightsByType.merge(roomTypeName(night), 1L, Long::sum);
        }
        for (String type : nightsByType.keySet()) {
            roomsByType.putIfAbsent(type, 0);
        }

        List<Map<String, Object>> byRoomType = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : roomsByType.entrySet()) {
            long nights = nightsByType.getOrDefault(entry.getKey(), 0L);
            long available = (long) entry.getValue() * days;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("roomType", entry.getKey());
            row.put("rooms", entry.getValue());
            row.put("nights", nights);
            row.put("percent", percent(nights, available));
            byRoomType.add(row);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalNights", stayNights.size());
        report.put("capacity", capacity);
        report.put("occupancyPercent", percent(stayNights.size(), (long) capacity * days));
        report.put("byDate", byDate);
        report.put("byRoomType", byRoomType);
        return report;
    }

    public Map<String, Object> getNightsReport(LocalDate startDate, LocalDate endDate) {
        List<StayNight> stayNights = stayNightRepository.findByStayDateBetween(startDate, endDate);

        BigDecimal totalRevenue = stayNights.stream()
                .map(StayNight::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, long[]> nightsByType = new TreeMap<>();
        Map<String, BigDecimal> revenueByType = new HashMap<>();
        for (StayNight night : stayNights) {
            String type = roomTypeName(night);
            nightsByType.computeIfAbsent(type, key -> new long[1])[0]++;
            revenueByType.merge(type, night.getPrice() == null ? BigDecimal.ZERO : night.getPrice(), BigDecimal::add);
        }

        List<Map<String, Object>> byRoomType = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : nightsByType.entrySet()) {
            BigDecimal revenue = revenueByType.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            long nights = entry.getValue()[0];
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("roomType", entry.getKey());
            row.put("nights", nights);
            row.put("revenue", revenue);
            row.put("averagePrice", average(revenue, nights));
            byRoomType.add(row);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalNights", stayNights.size());
        report.put("totalRevenue", totalRevenue);
        report.put("averagePricePerNight", average(totalRevenue, stayNights.size()));
        report.put("byRoomType", byRoomType);
        return report;
    }

    public Map<String, Object> getNsiReport(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        List<StayNight> stayNights = stayNightRepository.findByStayDateBetween(start, end);
        List<Booking> arrivals = bookingRepository.findAll().stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELLED)
                .filter(booking -> !booking.getCheckInDate().isBefore(start) && !booking.getCheckInDate().isAfter(end))
                .collect(Collectors.toList());

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("month", month.toString());
        report.put("arrivals", arrivals.size());
        report.put("foreignArrivals", arrivals.stream().filter(booking -> !isBulgarian(booking)).count());
        report.put("nights", stayNights.size());
        report.put("foreignNights", stayNights.stream().filter(night -> !isBulgarian(night.getBooking())).count());

        BigDecimal roomRevenue = stayNights.stream()
                .map(night -> night.getPrice() == null ? BigDecimal.ZERO : night.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal foreignRevenue = stayNights.stream()
                .filter(night -> !isBulgarian(night.getBooking()))
                .map(night -> night.getPrice() == null ? BigDecimal.ZERO : night.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.put("roomRevenue", roomRevenue);
        report.put("foreignRoomRevenue", foreignRevenue);

        Map<String, long[]> arrivalsByCountry = new TreeMap<>();
        Map<String, long[]> nightsByCountry = new TreeMap<>();
        Map<String, BigDecimal> revenueByCountry = new TreeMap<>();
        for (Booking booking : arrivals) {
            if (isBulgarian(booking)) continue;
            arrivalsByCountry.computeIfAbsent(countryName(booking), key -> new long[1])[0]++;
        }
        for (StayNight night : stayNights) {
            if (isBulgarian(night.getBooking())) continue;
            String country = countryName(night.getBooking());
            nightsByCountry.computeIfAbsent(country, key -> new long[1])[0]++;
            revenueByCountry.merge(country, night.getPrice() == null ? BigDecimal.ZERO : night.getPrice(), BigDecimal::add);
            arrivalsByCountry.putIfAbsent(country, new long[1]);
        }

        List<Map<String, Object>> byCountry = new ArrayList<>();
        for (String country : arrivalsByCountry.keySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("country", country);
            row.put("arrivals", arrivalsByCountry.get(country)[0]);
            row.put("nights", nightsByCountry.getOrDefault(country, new long[1])[0]);
            row.put("roomRevenue", revenueByCountry.getOrDefault(country, BigDecimal.ZERO));
            byCountry.add(row);
        }
        report.put("byCountry", byCountry);
        return report;
    }

    private String roomTypeName(StayNight night) {
        return night.getBooking().getRoom().getRoomType().getName();
    }

    private boolean isBulgarian(Booking booking) {
        String country = booking.getGuest().getCountry();
        if (country == null || country.isBlank()) {
            return true;
        }
        String normalized = country.trim().toLowerCase();
        return normalized.equals("българия") || normalized.equals("bulgaria") || normalized.equals("bg");
    }

    private String countryName(Booking booking) {
        String country = booking.getGuest().getCountry();
        return country == null || country.isBlank() ? "България" : country.trim();
    }

    private double percent(long part, long whole) {
        if (whole <= 0) {
            return 0;
        }
        return Math.round(part * 1000.0 / whole) / 10.0;
    }

    private BigDecimal average(BigDecimal total, long count) {
        if (count <= 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
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
                    StayPeriod.overlaps(b.getCheckInDate(), b.getCheckOutDate(), date, date.plusDays(1))
                );
                
                Booking booking = bookings.stream()
                    .filter(b -> b.getRoom().getId().equals(roomId) &&
                               StayPeriod.overlaps(b.getCheckInDate(), b.getCheckOutDate(), date, date.plusDays(1)))
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
                    roomStatus.put("checkInDate", booking.getCheckInDate().toString());
                    roomStatus.put("checkOutDate", booking.getCheckOutDate().toString());
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

