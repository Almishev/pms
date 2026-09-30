package com.hotel.pms.service;

import com.hotel.pms.model.entity.Room;
import com.hotel.pms.model.entity.RoomType;
import com.hotel.pms.model.enums.BookingStatus;
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

        if (roomNumber != null && !roomNumber.trim().isEmpty()) {
            String normalizedRoomNumber = roomNumber.trim();
            if (!room.getRoomNumber().equals(normalizedRoomNumber)) {
                if (roomRepository.findByRoomNumber(normalizedRoomNumber).isPresent()) {
                    throw new BusinessException("Room number already exists: " + normalizedRoomNumber);
                }
                room.setRoomNumber(normalizedRoomNumber);
            }
        }

        if (roomTypeId != null) {
            RoomType roomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new BusinessException("Room type not found with id: " + roomTypeId));
            room.setRoomType(roomType);
        }

        if (active != null) {
            room.setActive(active);
        }

        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        Room room = getRoomById(id);

        if (bookingRepository.existsByRoomId(id)) {
            throw new BusinessException("Cannot delete room with active bookings. Deactivate it instead.");
        }

        roomRepository.delete(room);
    }

    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    public List<RoomType> getActiveRoomTypes() {
        return roomTypeRepository.findByActiveTrue();
    }

    public RoomType createRoomType(String name, Integer capacity, java.math.BigDecimal basePrice) {
        String normalizedName = name == null ? null : name.trim();
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new BusinessException("Room type name is required.");
        }

        if (roomTypeRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
            throw new BusinessException("Room type already exists: " + normalizedName);
        }

        RoomType roomType = new RoomType();
        roomType.setName(normalizedName);
        roomType.setCapacity(capacity);
        roomType.setBasePrice(basePrice);
        roomType.setActive(true);

        return roomTypeRepository.save(roomType);
    }

    public RoomType updateRoomType(Long id, String name, Integer capacity, java.math.BigDecimal basePrice, Boolean active) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Room type not found with id: " + id));

        if (name != null && !name.trim().isEmpty()) {
            String normalizedName = name.trim();
            if (!normalizedName.equalsIgnoreCase(roomType.getName())
                    && roomTypeRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
                throw new BusinessException("Room type already exists: " + normalizedName);
            }

            roomType.setName(normalizedName);
        }

        if (capacity != null) {
            roomType.setCapacity(capacity);
        }

        if (basePrice != null) {
            roomType.setBasePrice(basePrice);
        }

        if (active != null) {
            roomType.setActive(active);
        }

        return roomTypeRepository.save(roomType);
    }

    public void deleteRoomType(Long id) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Room type not found with id: " + id));

        if (roomRepository.existsByRoomTypeId(id)) {
            throw new BusinessException("Не може да се изтрие тип, към който има стаи.");
        }

        roomTypeRepository.delete(roomType);
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
        LocalDate today = LocalDate.now();
        return bookingRepository.findByRoomIdAndStatusNot(roomId, BookingStatus.CANCELLED).stream()
                .noneMatch(booking -> StayPeriod.overlaps(
                        booking.getCheckInDate(),
                        StayPeriod.occupiedUntil(booking.getStatus(), booking.getCheckOutDate(), today),
                        checkInDate,
                        checkOutDate));
    }
}

