package ca.waaw.repository.shifts;

import ca.waaw.domain.shifts.Shifts;
import ca.waaw.dto.sqldtos.ShiftBatchSqlDto;
import ca.waaw.web.rest.utils.jpasqlqueries.ShiftQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftsRepository extends JpaRepository<Shifts, String> {

    @Query(value = ShiftQueries.lastUsedShiftId, nativeQuery = true)
    Optional<String> getLastUsedShiftId();

    @Query(value = ShiftQueries.lastUsedBatchId, nativeQuery = true)
    Optional<String> getLastUsedBatchId(String organizationId);

    Optional<Shifts> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    List<Shifts> findAllByUserIdAndStartBetweenAndDeleteFlag(String userId, Instant startRange, Instant endRange, boolean deleteFlag);

    List<Shifts> findAllByStartBetweenAndDeleteFlag(Instant startRange, Instant endRange, boolean deleteFlag);

    List<Shifts> findAllByUserIdAndStartAfterAndDeleteFlag(String userId, Instant startRange, boolean deleteFlag);

    Optional<List<Shifts>> findAllByBatchIdAndDeleteFlag(String batchId, boolean deleteFlag);

    List<Shifts> findAllByUserIdAndStartAfterOrderByStartAsc(String userId, Instant start);

    List<Shifts> findAllByLocationIdAndStartBetweenAndDeleteFlag(String locationId, Instant start, Instant end, boolean deleteFlag);

    @Query(value = ShiftQueries.singleUserBetweenDates)
    Optional<Shifts> getSingleByUserIdBetweenDates(String userId, Instant startRange, Instant endRange);

    @Query(value = ShiftQueries.allUsersBetweenDates)
    List<Shifts> getByUserIdBetweenDates(String userId, Instant startRange, Instant endRange);

    List<Shifts> findAllByOrganizationIdAndStartBetweenAndDeleteFlag(String organizationId, Instant startRange, Instant endRange, boolean deleteFlag);

    List<Shifts> findAllByLocationIdAndStartBetween(String locationId, Instant startRange, Instant endRange);

    @Query(value = ShiftQueries.organizationOrLocationBetweenDates)
    List<Shifts> getAllShiftsForOrganizationOrLocationBetweenDates(String organizationId, String locationId,
                                                                   Instant startRange, Instant endRange);

    @Query(value = ShiftQueries.upcomingOrOngoingShifts)
    List<Shifts> getAllUpcomingOrOngoingShifts(Instant start, Instant end);

    @Query(value = ShiftQueries.upcomingOrOngoingShiftsUser)
    Optional<Shifts> getAllUpcomingOrOngoingShifts(String userId, Instant start, Instant end);

    @Query(value = ShiftQueries.activeEmployeeCountBetweenDates)
    long getActiveEmployeesBetweenDates(String organizationId, Instant start, Instant end);

    @Query(value = ShiftQueries.searchAndFilterBatch, nativeQuery = true)
    Page<ShiftBatchSqlDto> getAllBatches(
            String searchKey, String organizationId, String locationId, String roleId, Instant start, Instant end,
            String shiftStatus, Pageable pageable
    );

    @Query(value = ShiftQueries.searchAndFilterBatchStatus, nativeQuery = true)
    Page<ShiftBatchSqlDto> getAllBatchesByStatus(
            String searchKey, String organizationId, String locationId, String roleId, Instant start, Instant end,
            String shiftStatus, String batchStatus, Pageable pageable
    );

    @Query(value = ShiftQueries.searchAndFilterAssignedBatch, nativeQuery = true)
    Page<ShiftBatchSqlDto> getAllBatchesAssigned(
            String searchKey, String organizationId, String locationId, String roleId, Instant start, Instant end,
            String shiftStatus, Pageable pageable
    );

}