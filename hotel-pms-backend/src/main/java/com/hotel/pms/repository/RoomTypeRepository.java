package com.hotel.pms.repository;

import com.hotel.pms.model.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByActiveTrue();
    Optional<RoomType> findByNameIgnoreCase(String name);
}

