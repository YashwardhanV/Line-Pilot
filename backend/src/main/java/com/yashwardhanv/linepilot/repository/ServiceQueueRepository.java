package com.yashwardhanv.linepilot.repository;

import com.yashwardhanv.linepilot.entity.ServiceQueue;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceQueueRepository extends JpaRepository<ServiceQueue, Long> {

    Optional<ServiceQueue> findByCode(String code);

    boolean existsByCode(String code);

    List<ServiceQueue> findAllByOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from ServiceQueue q where q.id = :id")
    Optional<ServiceQueue> findByIdForUpdate(@Param("id") Long id);
}
