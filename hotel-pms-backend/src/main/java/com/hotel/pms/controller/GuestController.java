package com.hotel.pms.controller;

import com.hotel.pms.model.dto.GuestDto;
import com.hotel.pms.model.entity.Guest;
import com.hotel.pms.service.GuestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guests")
@CrossOrigin(origins = "*")
public class GuestController {
    @Autowired
    private GuestService guestService;

    @GetMapping
    public ResponseEntity<List<Guest>> getAllGuests() {
        return ResponseEntity.ok(guestService.getAllGuests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Guest> getGuestById(@PathVariable Long id) {
        return ResponseEntity.ok(guestService.getGuestById(id));
    }

    @PostMapping
    public ResponseEntity<Guest> createGuest(@Valid @RequestBody GuestDto dto) {
        return ResponseEntity.ok(guestService.createGuest(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Guest> updateGuest(@PathVariable Long id, @Valid @RequestBody GuestDto dto) {
        return ResponseEntity.ok(guestService.updateGuest(id, dto));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Guest>> searchGuests(@RequestParam String query) {
        return ResponseEntity.ok(guestService.searchGuests(query));
    }
}

