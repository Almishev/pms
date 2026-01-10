package com.hotel.pms.service;

import com.hotel.pms.model.entity.Booking;
import com.hotel.pms.model.entity.StayNight;
import com.hotel.pms.repository.StayNightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class StayService {
    @Autowired
    private StayNightRepository stayNightRepository;

    @Transactional
    public void generateStayNights(Booking booking) {
        generateStayNights(booking, null);
    }

    @Transactional
    public void generateStayNights(Booking booking, BigDecimal customPricePerNight) {
        // Delete existing stay nights for this booking
        stayNightRepository.findByBookingId(booking.getId()).forEach(stayNightRepository::delete);

        LocalDate currentDate = booking.getCheckInDate();
        // Use custom price if provided, otherwise use room type base price
        BigDecimal pricePerNight = (customPricePerNight != null && customPricePerNight.compareTo(java.math.BigDecimal.ZERO) > 0)
                ? customPricePerNight
                : booking.getRoom().getRoomType().getBasePrice();

        while (currentDate.isBefore(booking.getCheckOutDate())) {
            StayNight stayNight = new StayNight();
            stayNight.setBooking(booking);
            stayNight.setStayDate(currentDate);
            stayNight.setPrice(pricePerNight);
            stayNightRepository.save(stayNight);

            currentDate = currentDate.plusDays(1);
        }
    }

    public List<StayNight> getStayNightsByBooking(Long bookingId) {
        return stayNightRepository.findByBookingId(bookingId);
    }

    public List<StayNight> getStayNightsByDateRange(LocalDate startDate, LocalDate endDate) {
        return stayNightRepository.findByStayDateBetween(startDate, endDate);
    }

    public long getOccupancyCount(LocalDate date) {
        return stayNightRepository.countByStayDate(date);
    }

    @Transactional
    public void updateStayNightsPrice(Long bookingId, BigDecimal newPricePerNight) {
        if (newPricePerNight == null || newPricePerNight.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new com.hotel.pms.exception.BusinessException("Price must be positive");
        }

        List<StayNight> stayNights = stayNightRepository.findByBookingId(bookingId);
        if (stayNights.isEmpty()) {
            throw new com.hotel.pms.exception.BusinessException("No stay nights found for this booking");
        }

        for (StayNight stayNight : stayNights) {
            stayNight.setPrice(newPricePerNight);
            stayNightRepository.save(stayNight);
        }
    }
}

