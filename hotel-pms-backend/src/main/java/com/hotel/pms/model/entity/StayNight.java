package com.hotel.pms.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stay_night", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"booking_id", "stay_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StayNight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}

