package com.hotel.pms.controller;

import com.hotel.pms.model.entity.Room;
import com.hotel.pms.model.entity.RoomType;
import com.hotel.pms.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {
    @Autowired
    private RoomService roomService;

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        if (checkInDate != null && checkOutDate != null) {
            return ResponseEntity.ok(roomService.getAvailableRooms(checkInDate, checkOutDate));
        }
        return ResponseEntity.ok(roomService.getActiveRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Room> createRoom(
            @RequestParam String roomNumber,
            @RequestParam Long roomTypeId) {
        return ResponseEntity.ok(roomService.createRoom(roomNumber, roomTypeId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Room> updateRoom(
            @PathVariable Long id,
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(roomService.updateRoom(id, roomNumber, roomTypeId, active));
    }

    @GetMapping("/types")
    public ResponseEntity<List<RoomType>> getAllRoomTypes() {
        return ResponseEntity.ok(roomService.getActiveRoomTypes());
    }

    @PostMapping("/types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomType> createRoomType(
            @RequestParam String name,
            @RequestParam Integer capacity,
            @RequestParam java.math.BigDecimal basePrice) {
        return ResponseEntity.ok(roomService.createRoomType(name, capacity, basePrice));
    }
}

