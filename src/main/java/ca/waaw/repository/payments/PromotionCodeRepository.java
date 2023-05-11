package ca.waaw.repository.payments;

import ca.waaw.domain.payments.PromotionCode;
import ca.waaw.enumration.payment.PromoCodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionCodeRepository extends JpaRepository<PromotionCode, String> {

    Optional<PromotionCode> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<PromotionCode> findOneByCodeAndDeleteFlag(String code, boolean deleteFlag);

    Optional<PromotionCode> findOneByCodeAndTypeAndDeleteFlag(String code, PromoCodeType type, boolean deleteFlag);

    Page<PromotionCode> getAllByConditionalExpiredAndDeleted(Boolean includeDeleted, Boolean includeExpired, Pageable pageable);

}