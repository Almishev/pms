package com.hotel.pms.service;

import com.hotel.pms.model.dto.CreateBookingDto;
import com.hotel.pms.model.entity.Booking;
import com.hotel.pms.model.entity.Guest;
import com.hotel.pms.model.entity.Room;
import com.hotel.pms.model.entity.User;
import com.hotel.pms.model.enums.BookingStatus;
import com.hotel.pms.repository.BookingRepository;
import com.hotel.pms.repository.GuestRepository;
import com.hotel.pms.repository.RoomRepository;
import com.hotel.pms.repository.UserRepository;
import com.hotel.pms.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StayService stayService;

    @Transactional
    public Booking createBooking(CreateBookingDto dto) {
        // Validate dates
        if (dto.getCheckInDate().isAfter(dto.getCheckOutDate()) || 
            dto.getCheckInDate().isEqual(dto.getCheckOutDate())) {
            throw new BusinessException("Check-out date must be after check-in date");
        }

        if (dto.getCheckInDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Check-in date cannot be in the past");
        }

        // Get room and guest
        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new BusinessException("Room not found with id: " + dto.getRoomId()));

        if (!room.getActive()) {
            throw new BusinessException("Room is not active");
        }

        Guest guest = guestRepository.findById(dto.getGuestId())
                .orElseThrow(() -> new BusinessException("Guest not found with id: " + dto.getGuestId()));

        // Check for overlapping bookings
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                dto.getRoomId(), dto.getCheckInDate(), dto.getCheckOutDate());

        if (!overlapping.isEmpty()) {
            throw new BusinessException("Room is already booked for the selected dates");
        }

        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Create booking
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setGuest(guest);
        booking.setCheckInDate(dto.getCheckInDate());
        booking.setCheckOutDate(dto.getCheckOutDate());
        booking.setStatus(BookingStatus.BOOKED);
        booking.setCreatedBy(user);

        booking = bookingRepository.save(booking);

        // Generate stay nights with custom price if provided
        stayService.generateStayNights(booking, dto.getCustomPricePerNight());

        return booking;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Booking not found with id: " + id));
    }

    @Transactional
    public Booking checkIn(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BusinessException("Only booked reservations can be checked in");
        }

        if (!booking.getCheckInDate().equals(LocalDate.now()) && 
            booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Cannot check in before check-in date");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking checkOut(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new BusinessException("Only checked-in reservations can be checked out");
        }

        booking.setStatus(BookingStatus.CHECKED_OUT);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        
        if (booking.getStatus() == BookingStatus.CHECKED_OUT) {
            throw new BusinessException("Cannot cancel already checked-out reservation");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsByDateRange(LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findByCheckInDateBetweenOrCheckOutDateBetween(
                startDate, endDate, startDate, endDate);
    }
}

