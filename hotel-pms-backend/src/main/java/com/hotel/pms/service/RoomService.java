package com.hotel.pms.service;

import com.hotel.pms.model.entity.Room;
import com.hotel.pms.model.entity.RoomType;
import com.hotel.pms.model.entity.Booking;
import com.hotel.pms.repository.RoomRepository;
import com.hotel.pms.repository.RoomTypeRepository;
import com.hotel.pms.repository.BookingRepository;
import com.hotel.pms.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {
    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private BookingRepository bookingRepository;

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> getActiveRooms() {
        return roomRepository.findByActiveTrue();
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Room not found with id: " + id));
    }

    public Room createRoom(String roomNumber, Long roomTypeId) {
        if (roomRepository.findByRoomNumber(roomNumber).isPresent()) {
            throw new BusinessException("Room number already exists: " + roomNumber);
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new BusinessException("Room type not found with id: " + roomTypeId));

        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setRoomType(roomType);
        room.setActive(true);

        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, String roomNumber, Long roomTypeId, Boolean active) {
        Room room = getRoomById(id);
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new BusinessException("Room type not found with id: " + roomTypeId));

        if (!room.getRoomNumber().equals(roomNumber)) {
            if (roomRepository.findByRoomNumber(roomNumber).isPresent()) {
                throw new BusinessException("Room number already exists: " + roomNumber);
            }
            room.setRoomNumber(roomNumber);
        }

        room.setRoomType(roomType);
        if (active != null) {
            room.setActive(active);
        }

        return roomRepository.save(room);
    }

    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    public List<RoomType> getActiveRoomTypes() {
        return roomTypeRepository.findByActiveTrue();
    }

    public RoomType createRoomType(String name, Integer capacity, java.math.BigDecimal basePrice) {
        RoomType roomType = new RoomType();
        roomType.setName(name);
        roomType.setCapacity(capacity);
        roomType.setBasePrice(basePrice);
        roomType.setActive(true);

        return roomTypeRepository.save(roomType);
    }

    public List<Room> getAvailableRooms(LocalDate checkInDate, LocalDate checkOutDate) {
        List<Room> allActiveRooms = roomRepository.findByActiveTrue();
        
        if (checkInDate == null || checkOutDate == null) {
            return allActiveRooms;
        }

        return allActiveRooms.stream()
                .filter(room -> isRoomAvailable(room.getId(), checkInDate, checkOutDate))
                .collect(Collectors.toList());
    }

    private boolean isRoomAvailable(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                roomId, checkInDate, checkOutDate);
        return overlapping.isEmpty();
    }
}

