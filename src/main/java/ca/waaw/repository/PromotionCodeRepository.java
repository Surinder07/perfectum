package ca.waaw.repository;

import ca.waaw.domain.PromotionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionCodeRepository extends JpaRepository<PromotionCode, String> {

    Optional<PromotionCode> findOneByIdAndDeleteFlag(String id, boolean deleteFlag);

    Optional<PromotionCode> findOneByCodeAndDeleteFlag(String code, boolean deleteFlag);

}
