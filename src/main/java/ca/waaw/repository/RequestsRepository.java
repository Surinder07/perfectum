package ca.waaw.repository;

import ca.waaw.domain.Requests;
import ca.waaw.enumration.RequestStatus;
import ca.waaw.enumration.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestsRepository extends JpaRepository<Requests, String> {

    @Query(value = "SELECT waaw_id from requests WHERE waaw_id IS NOT NULL ORDER BY created_date DESC LIMIT 1",
            nativeQuery = true)
    Optional<String> getLastUsedWaawId();

    @Query(value = "SELECT t FROM Requests t WHERE t.userId = ?1 AND t.type = ?2 AND t.start BETWEEN ?3 AND ?4 AND t.deleteFlag = FALSE")
    Optional<Requests> getOneForUserBetweenDatesAndByType(String userId, RequestType type, Instant start, Instant end);

    List<Requests> findAllByStartBetweenAndDeleteFlag(Instant start, Instant end, boolean deleteFlag);

    List<Requests> findAllByCreatedDateBetweenAndTypeAndDeleteFlag(Instant start, Instant end, RequestType type, boolean deleteFlag);

    List<Requests> findAllByOrganizationIdAndDeleteFlagAndStatusIn(String organizationId, boolean delFlg, List<RequestStatus> status);

    List<Requests> findAllByUserIdAndDeleteFlagAndStatusIn(String userId, boolean delFlg, List<RequestStatus> status);

    List<Requests> findAllByLocationIdAndStartBetweenAndDeleteFlag(String locationId, Instant start, Instant end, boolean deleteFlag);

    @Query(value = "SELECT t FROM Requests t WHERE (?1 BETWEEN t.start AND t.end) OR (?2 BETWEEN t.start AND t.end) " +
            "OR (t.start BETWEEN ?1 AND ?2) OR (t.end BETWEEN ?1 AND ?2) AND t.deleteFlag = FALSE")
    List<Requests> getOverlappingForDates(Instant start, Instant end, boolean deleteFlag);

}