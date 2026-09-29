package com.hotel.pms.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "room")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", unique = true, nullable = false, length = 10)
    private String roomNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "nightly_charge", precision = 10, scale = 2)
    private BigDecimal nightlyCharge = BigDecimal.ZERO;

    @Column(name = "restaurant_charge", precision = 10, scale = 2)
    private BigDecimal restaurantCharge = BigDecimal.ZERO;

    @Column(name = "account_balance", precision = 10, scale = 2)
    private BigDecimal accountBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean active = true;
}

