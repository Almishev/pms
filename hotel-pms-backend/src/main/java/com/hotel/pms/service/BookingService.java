package com.hotel.pms.service;

import com.hotel.pms.model.dto.CreateBookingDto;
import com.hotel.pms.model.dto.MoveBookingDto;
import com.hotel.pms.model.entity.Booking;
import com.hotel.pms.model.entity.Guest;
import com.hotel.pms.model.entity.Room;
import com.hotel.pms.model.entity.StayNight;
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

import java.math.BigDecimal;
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

        Room room = roomRepository.findByIdForUpdate(dto.getRoomId())
                .orElseThrow(() -> new BusinessException("Room not found with id: " + dto.getRoomId()));

        if (!room.getActive()) {
            throw new BusinessException("Room is not active");
        }

        Guest guest = resolveGuest(dto);

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
        booking.setRestaurantCharge(BigDecimal.ZERO);
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
        
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BusinessException("Only booked reservations can be cancelled");
        }

        stayService.deleteStayNights(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking moveBooking(Long bookingId, MoveBookingDto dto) {
        if (dto.getCheckInDate() == null || dto.getCheckOutDate() == null
                || !dto.getCheckOutDate().isAfter(dto.getCheckInDate())) {
            throw new BusinessException("Check-out date must be after check-in date");
        }

        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.BOOKED && booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new BusinessException("Само активна резервация може да се мести");
        }
        if (booking.getStatus() == BookingStatus.CHECKED_IN && dto.getCheckInDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Настанен гост не може да бъде преместен с бъдещо пристигане");
        }

        Room room = roomRepository.findByIdForUpdate(dto.getRoomId())
                .orElseThrow(() -> new BusinessException("Room not found with id: " + dto.getRoomId()));
        if (!Boolean.TRUE.equals(room.getActive())) {
            throw new BusinessException("Room is not active");
        }

        List<Booking> overlapping = bookingRepository.findOverlappingBookingsExcluding(
                dto.getRoomId(), dto.getCheckInDate(), dto.getCheckOutDate(), bookingId);
        if (!overlapping.isEmpty()) {
            throw new BusinessException("Стаята е заета за избраните дати");
        }

        BigDecimal price = stayService.getStayNightsByBooking(bookingId).stream()
                .map(StayNight::getPrice)
                .filter(nightPrice -> nightPrice != null)
                .findFirst()
                .orElse(null);

        booking.setRoom(room);
        booking.setCheckInDate(dto.getCheckInDate());
        booking.setCheckOutDate(dto.getCheckOutDate());
        booking = bookingRepository.save(booking);
        stayService.generateStayNights(booking, price);
        return booking;
    }

    @Transactional
    public Booking updateRestaurantCharge(Long bookingId, BigDecimal restaurantCharge) {
        if (restaurantCharge == null || restaurantCharge.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Restaurant charge cannot be negative");
        }

        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.CHECKED_OUT) {
            throw new BusinessException("Cannot change restaurant charge for this reservation");
        }

        booking.setRestaurantCharge(restaurantCharge);
        return bookingRepository.save(booking);
    }

    public List<com.hotel.pms.model.dto.OpenFolioDto> getOpenFolios() {
        return bookingRepository.findByStatus(BookingStatus.CHECKED_IN).stream()
                .map(booking -> {
                    BigDecimal nightsTotal = stayService.getStayNightsByBooking(booking.getId()).stream()
                            .map(stayNight -> stayNight.getPrice())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal restaurant = booking.getRestaurantCharge() == null
                            ? BigDecimal.ZERO
                            : booking.getRestaurantCharge();

                    com.hotel.pms.model.dto.OpenFolioDto folio = new com.hotel.pms.model.dto.OpenFolioDto();
                    folio.setRoomId(booking.getRoom().getId());
                    folio.setBookingId(booking.getId());
                    folio.setGuestName(booking.getGuest().getFirstName() + " " + booking.getGuest().getLastName());
                    folio.setNightsTotal(nightsTotal);
                    folio.setRestaurantCharge(restaurant);
                    folio.setTotal(nightsTotal.add(restaurant));
                    return folio;
                })
                .toList();
    }

    private Guest resolveGuest(CreateBookingDto dto) {
        if (dto.getGuestId() != null) {
            return guestRepository.findById(dto.getGuestId())
                    .orElseThrow(() -> new BusinessException("Guest not found with id: " + dto.getGuestId()));
        }

        if (dto.getGuestFirstName() == null || dto.getGuestFirstName().isBlank()
                || dto.getGuestLastName() == null || dto.getGuestLastName().isBlank()) {
            throw new BusinessException("Guest name is required");
        }

        Guest guest = new Guest();
        guest.setFirstName(dto.getGuestFirstName().trim());
        guest.setLastName(dto.getGuestLastName().trim());
        guest.setPhone(dto.getGuestPhone());
        guest.setIdNumber(dto.getGuestIdNumber());
        String country = dto.getGuestCountry();
        guest.setCountry(country == null || country.isBlank() ? "България" : country.trim());
        return guestRepository.save(guest);
    }

    public List<Booking> getBookingsByDateRange(LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findByCheckInDateBetweenOrCheckOutDateBetween(
                startDate, endDate, startDate, endDate);
    }
}

