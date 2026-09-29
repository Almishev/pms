package com.hotel.pms.service;

import com.hotel.pms.exception.BusinessException;
import com.hotel.pms.model.entity.Booking;
import com.hotel.pms.model.enums.BookingStatus;
import com.hotel.pms.repository.BookingRepository;
import com.hotel.pms.repository.GuestRepository;
import com.hotel.pms.repository.RoomRepository;
import com.hotel.pms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StayService stayService;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void cancelBooking_rejectsCheckedInReservation() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.CHECKED_IN);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class, () -> bookingService.cancelBooking(1L));
        verify(stayService, never()).deleteStayNights(1L);
    }

    @Test
    void cancelBooking_deletesStayNightsForBookedReservation() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.BOOKED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking cancelled = bookingService.cancelBooking(1L);

        verify(stayService).deleteStayNights(1L);
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
    }
}
