package ca.waaw.repository;

import ca.waaw.domain.Shifts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftsRepository extends JpaRepository<Shifts, String> {

    @Query(value = "SELECT waaw_id from shifts WHERE waaw_id IS NOT NULL ORDER BY waaw_id DESC LIMIT 1",
            nativeQuery = true)
    Optional<String> getLastUsedCustomId();

    Optional<Shifts> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    List<Shifts> findAllByUserIdAndStartBetweenAndDeleteFlag(String userId, Instant startRange, Instant endRange, boolean deleteFlag);

    List<Shifts> findAllByStartBetweenAndDeleteFlag(Instant startRange, Instant endRange, boolean deleteFlag);

    List<Shifts> findAllByUserIdAndStartAfterAndDeleteFlag(String userId, Instant startRange, boolean deleteFlag);

    List<Shifts> findAllByBatchIdAndDeleteFlag(String batchId, boolean deleteFlag);

    List<Shifts> findAllByUserIdAndStartAfterOrderByCreatedDateAsc(String userId, Instant start);

    List<Shifts> findAllByLocationIdAndStartBetweenAndDeleteFlag(String locationId, Instant start, Instant end, boolean deleteFlag);

    /*
     * Will check for both start and end date
     */
    @Query(value = "SELECT s FROM Shifts s WHERE s.userId = ?1 AND (s.start BETWEEN ?2 AND ?3 OR " +
            "s.end BETWEEN ?2 AND ?3) AND s.deleteFlag = FALSE")
    Optional<Shifts> getSingleByUserIdBetweenDates(String userId, Instant startRange, Instant endRange);

    @Query(value = "SELECT s FROM Shifts s WHERE s.userId = ?1 AND (s.start BETWEEN ?2 AND ?3 OR " +
            "s.end BETWEEN ?2 AND ?3) AND s.deleteFlag = FALSE")
    List<Shifts> getByUserIdBetweenDates(String userId, Instant startRange, Instant endRange);

    List<Shifts> findAllByLocationIdAndStartBetween(String locationId, Instant startRange, Instant endRange);

    List<Shifts> findAllByOrganizationIdAndStartBetweenAndDeleteFlag(String organizationId, Instant startRange, Instant endRange, boolean deleteFlag);

    @Query(value = "SELECT s FROM Shifts s WHERE s.start < ?1 AND s.end > ?2 AND s.deleteFlag = FALSE")
    List<Shifts> getAllUpcomingOrOngoingShifts(Instant start, Instant end);

    @Query(value = "SELECT s FROM Shifts s WHERE s.userId = ?1 AND s.start < ?2 AND s.end > ?3 AND " +
            "s.shiftStatus = 'RELEASED' AND s.deleteFlag = FALSE")
    Optional<Shifts> getAllUpcomingOrOngoingShifts(String userId, Instant start, Instant end);

    @Query(value = "SELECT COUNT(DISTINCT s.userId) FROM Shifts s WHERE s.organizationId = ?1 AND " +
            "(s.start BETWEEN ?2 AND ?3) AND s.shiftStatus = 'RELEASED' AND s.deleteFlag = FALSE")
    long getActiveEmployeesBetweenDates(String organizationId, Instant start, Instant end);

}