package com.hotel.pms.service;

import com.hotel.pms.exception.BusinessException;
import com.hotel.pms.model.entity.Room;
import com.hotel.pms.repository.BookingRepository;
import com.hotel.pms.repository.RoomRepository;
import com.hotel.pms.repository.RoomTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    void deleteRoom_shouldDeleteRoomWhenNoBookingsExist() {
        Room room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");
        room.setActive(true);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsByRoomId(1L)).thenReturn(false);

        roomService.deleteRoom(1L);

        verify(roomRepository).delete(room);
    }

    @Test
    void deleteRoom_shouldRejectIfRoomHasBookings() {
        Room room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");
        room.setActive(true);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsByRoomId(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> roomService.deleteRoom(1L));
        verify(roomRepository, never()).delete(any());
    }
}
