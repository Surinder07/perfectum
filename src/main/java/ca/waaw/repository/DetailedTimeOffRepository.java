package ca.waaw.repository;

import ca.waaw.domain.joined.DetailedTimeOff;
import ca.waaw.enumration.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface DetailedTimeOffRepository extends JpaRepository<DetailedTimeOff, String> {

    /**
     * If status is null all entities will be returned
     */
    Page<DetailedTimeOff> getByUserIdAfterDate(String userId, Instant start, EntityStatus status, Pageable pageable);

    /**
     * If status is null all entities will be returned
     */
    Page<DetailedTimeOff> getByLocationIdAfterDate(String locationId, Instant start, EntityStatus status, Pageable pageable);

    /**
     * If status is null all entities will be returned
     */
    Page<DetailedTimeOff> getByOrganizationIdAfterDate(String organizationId, Instant start, EntityStatus status, Pageable pageable);

    /**
     * If status is null all entities will be returned
     */
    Page<DetailedTimeOff> getByUserIdBetweenDates(String userId, Instant start, Instant end, EntityStatus status, Pageable pageable);

    /**
     * If status is null all entities will be returned
     */
    Page<DetailedTimeOff> getByLocationIdBetweenDates(String locationId, Instant start, Instant end, EntityStatus status, Pageable pageable);

    /**
     * If status is null all entities will be returned
     */
    Page<DetailedTimeOff> getByOrganizationIdBetweenDates(String organizationId, Instant start, Instant end, EntityStatus status, Pageable pageable);

}