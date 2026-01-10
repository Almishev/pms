package com.hotel.pms.service;

import com.hotel.pms.model.dto.GuestDto;
import com.hotel.pms.model.entity.Guest;
import com.hotel.pms.repository.GuestRepository;
import com.hotel.pms.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuestService {
    @Autowired
    private GuestRepository guestRepository;

    public List<Guest> getAllGuests() {
        return guestRepository.findAll();
    }

    public Guest getGuestById(Long id) {
        return guestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Guest not found with id: " + id));
    }

    public Guest createGuest(GuestDto dto) {
        Guest guest = new Guest();
        guest.setFirstName(dto.getFirstName());
        guest.setLastName(dto.getLastName());
        guest.setPhone(dto.getPhone());
        guest.setIdNumber(dto.getIdNumber());

        return guestRepository.save(guest);
    }

    public Guest updateGuest(Long id, GuestDto dto) {
        Guest guest = getGuestById(id);
        guest.setFirstName(dto.getFirstName());
        guest.setLastName(dto.getLastName());
        guest.setPhone(dto.getPhone());
        guest.setIdNumber(dto.getIdNumber());

        return guestRepository.save(guest);
    }

    public List<Guest> searchGuests(String query) {
        return guestRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query);
    }
}

