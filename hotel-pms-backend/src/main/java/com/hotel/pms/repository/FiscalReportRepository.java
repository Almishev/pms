package com.hotel.pms.repository;

import com.hotel.pms.model.entity.FiscalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FiscalReportRepository extends JpaRepository<FiscalReport, Long> {
    List<FiscalReport> findByReportTypeOrderByFiscalDateDesc(FiscalReport.ReportType reportType);
    
    @Query("SELECT fr FROM FiscalReport fr WHERE fr.fiscalDate BETWEEN :startDate AND :endDate ORDER BY fr.fiscalDate DESC")
    List<FiscalReport> findByFiscalDateBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    List<FiscalReport> findAllByOrderByFiscalDateDesc();
}

