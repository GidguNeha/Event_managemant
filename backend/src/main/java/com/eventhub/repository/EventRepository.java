package com.eventhub.repository;

import com.eventhub.model.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);
    List<Event> findByOrganizerIdOrderByEventDateAsc(Long organizerId);
    List<Event> findByNameContainingIgnoreCaseOrderByEventDateAsc(String name);
    Optional<Event> findByNameIgnoreCase(String name);
}
