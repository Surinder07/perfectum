package ca.waaw.repository.requests;

import ca.waaw.domain.requests.DetailedRequests;
import ca.waaw.enumration.request.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DetailedRequestsRepository extends JpaRepository<DetailedRequests, String> {

    Optional<DetailedRequests> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    @Query(value = "SELECT r FROM DetailedRequests r WHERE (?1 IS NULL OR r.organizationId = ?1) AND " +
            "(?2 IS NULL OR r.location.id = ?2) AND (?3 IS NULL OR r.user.id = ?3) AND " +
            "(?4 IS NULL OR r.type = ?4) AND (?5 IS NULL OR r.status = ?5) AND (?6 IS NULL OR " +
            "(r.user.firstName LIKE CONCAT('%', ?6, '%') OR r.user.lastName LIKE CONCAT('%', ?6, '%'))) " +
            "AND (?7 IS TRUE OR r.locationRole.adminRights = FALSE) AND ((?8 IS NULL OR ?9 IS NULL) OR " +
            "(r.createdDate BETWEEN ?8 AND ?9)) AND r.deleteFlag = FALSE ORDER BY r.createdDate DESC")
    Page<DetailedRequests> searchAndFilter(String organizationId, String locationId, String userId, String type,
                                           String status, String searchKey, boolean isAdmin, Instant start,
                                           Instant end, Pageable pageable);

    List<DetailedRequests> findAllByLocation_idAndDeleteFlagAndStatusIn(String locationId, boolean deleteFlag, List<RequestStatus> status);

}