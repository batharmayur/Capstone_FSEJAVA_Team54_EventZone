package com.eventzone.backend.repository;

import com.eventzone.backend.model.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, UUID> {

    @Query("select t from TicketCategory t join fetch t.event e join fetch e.category where t.id = :id")
    Optional<TicketCategory> findWithEventById(@Param("id") UUID id);

    /**
     * Atomically takes seats if enough remain. A single conditional UPDATE keeps concurrent bookings from
     * overselling. Returns the number of rows changed: 1 on success, 0 if there were not enough seats.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TicketCategory t set t.availableSeats = t.availableSeats - :qty "
            + "where t.id = :id and t.availableSeats >= :qty")
    int reserveSeats(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TicketCategory t set t.availableSeats = t.availableSeats + :qty where t.id = :id")
    int releaseSeats(@Param("id") UUID id, @Param("qty") int qty);

    @Query("select t.availableSeats from TicketCategory t where t.id = :id")
    int findAvailableSeats(@Param("id") UUID id);

    /**
     * Atomically edits a category and shifts available seats by the change in total, so a concurrent booking is never
     * overwritten. Returns 0 (and changes nothing) if the new total is below the seats already booked.
     * availableSeats is assigned before totalSeats so it is computed from the old total.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TicketCategory t set t.name = :name, t.price = :price, "
            + "t.availableSeats = t.availableSeats + (:total - t.totalSeats), t.totalSeats = :total "
            + "where t.id = :id and (t.totalSeats - t.availableSeats) <= :total")
    int updateDetails(@Param("id") UUID id, @Param("name") String name, @Param("price") BigDecimal price,
                      @Param("total") int total);
}
