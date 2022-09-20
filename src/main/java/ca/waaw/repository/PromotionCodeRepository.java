package ca.waaw.repository;

import ca.waaw.domain.PromotionCode;
import ca.waaw.enumration.PromoCodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionCodeRepository extends JpaRepository<PromotionCode, String> {

    Optional<PromotionCode> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<PromotionCode> findOneByCodeAndDeleteFlag(String code, boolean deleteFlag);

    Optional<PromotionCode> findOneByCodeAndTypeAndDeleteFlag(String code, PromoCodeType type, boolean deleteFlag);

    Page<PromotionCode> getAllByConditionalExpiredAndDeleted(Boolean includeDeleted, Boolean includeExpired, Pageable pageable);

}
