package com.hotel.pms.controller;

import com.hotel.pms.model.dto.OpenRoomDto;
import com.hotel.pms.model.dto.RoomChargeRequest;
import com.hotel.pms.model.dto.RoomChargeResponse;
import com.hotel.pms.model.dto.RoomChargeStornoRequest;
import com.hotel.pms.service.RestaurantFolioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integration")
public class IntegrationController {
    @Autowired
    private RestaurantFolioService restaurantFolioService;

    @GetMapping("/open-rooms")
    public ResponseEntity<List<OpenRoomDto>> openRooms() {
        return ResponseEntity.ok(restaurantFolioService.listOpenRooms());
    }

    @PostMapping("/room-charges")
    public ResponseEntity<RoomChargeResponse> postRoomCharge(@Valid @RequestBody RoomChargeRequest request) {
        return ResponseEntity.ok(restaurantFolioService.postRoomCharge(request));
    }

    @PostMapping("/room-charges/{billId}/storno")
    public ResponseEntity<RoomChargeResponse> stornoRoomCharge(
            @PathVariable String billId,
            @Valid @RequestBody RoomChargeStornoRequest request) {
        return ResponseEntity.ok(restaurantFolioService.stornoRoomCharge(billId, request));
    }
}
