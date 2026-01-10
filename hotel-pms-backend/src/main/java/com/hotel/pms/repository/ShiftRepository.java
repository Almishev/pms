package com.hotel.pms.repository;

import com.hotel.pms.model.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    @Query("SELECT s FROM Shift s WHERE s.user.id = :userId AND s.endTime IS NULL ORDER BY s.startTime DESC")
    Optional<Shift> findActiveShiftByUserId(@Param("userId") Long userId);
    
    @Query("SELECT s FROM Shift s WHERE s.startTime BETWEEN :startDate AND :endDate")
    List<Shift> findByStartTimeBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}

