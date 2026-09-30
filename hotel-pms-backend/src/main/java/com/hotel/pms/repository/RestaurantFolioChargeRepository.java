package com.hotel.pms.repository;

import com.hotel.pms.model.entity.RestaurantFolioCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantFolioChargeRepository extends JpaRepository<RestaurantFolioCharge, Long> {
    Optional<RestaurantFolioCharge> findByExternalBillId(String externalBillId);

    List<RestaurantFolioCharge> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    boolean existsByBookingId(Long bookingId);
}
