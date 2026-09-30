package com.hotel.pms.model.entity;

import com.hotel.pms.model.enums.FolioChargeSource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "restaurant_folio_charge")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantFolioCharge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "external_bill_id", unique = true, length = 64)
    private String externalBillId;

    @Column(name = "table_name", length = 80)
    private String tableName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "reversed_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal reversedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FolioChargeSource source;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "restaurant_folio_storno_key", joinColumns = @JoinColumn(name = "charge_id"))
    @Column(name = "storno_key", nullable = false, length = 64)
    private Set<String> appliedStornoKeys = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (reversedAmount == null) {
            reversedAmount = BigDecimal.ZERO;
        }
        if (appliedStornoKeys == null) {
            appliedStornoKeys = new HashSet<>();
        }
    }

    public BigDecimal activeAmount() {
        BigDecimal total = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal reversed = reversedAmount == null ? BigDecimal.ZERO : reversedAmount;
        return total.subtract(reversed);
    }
}
