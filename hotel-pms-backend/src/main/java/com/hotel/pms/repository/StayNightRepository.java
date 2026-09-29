package com.hotel.pms.repository;

import com.hotel.pms.model.entity.StayNight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StayNightRepository extends JpaRepository<StayNight, Long> {
    List<StayNight> findByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);
    
    @Query("SELECT sn FROM StayNight sn WHERE sn.stayDate BETWEEN :startDate AND :endDate")
    List<StayNight> findByStayDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT COUNT(sn) FROM StayNight sn WHERE sn.stayDate = :date")
    long countByStayDate(@Param("date") LocalDate date);
}

