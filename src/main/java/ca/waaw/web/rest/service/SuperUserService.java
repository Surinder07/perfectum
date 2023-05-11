package ca.waaw.web.rest.service;

import ca.waaw.domain.payments.PromotionCode;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.payments.PromotionCodeDto;
import ca.waaw.enumration.user.Authority;
import ca.waaw.enumration.payment.PromoCodeType;
import ca.waaw.repository.payments.PromotionCodeRepository;
import ca.waaw.web.rest.errors.exceptions.EntityAlreadyExistsException;
import ca.waaw.web.rest.errors.exceptions.EntityNotFoundException;
import ca.waaw.web.rest.utils.CommonUtils;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@AllArgsConstructor
public class SuperUserService {

    private final Logger log = LogManager.getLogger(SuperUserService.class);

    private final PromotionCodeRepository promotionCodeRepository;

    public void addNewCode(PromotionCodeDto promotionCodeDto) {
        CommonUtils.checkRoleAuthorization(Authority.SUPER_USER);
        promotionCodeRepository.findOneByCodeAndDeleteFlag(promotionCodeDto.getCode(), false)
                .filter(promotionCode -> promotionCode.getExpiryDate().isAfter(Instant.now()))
                .ifPresent(code -> {
                    log.debug("Promo code {} already exists", code);
                    throw new EntityAlreadyExistsException("promo code", "value", code.getCode());
                });
        PromotionCode promotionCode = PromotionCode.builder()
                .type(PromoCodeType.valueOf(promotionCodeDto.getType()))
                .code(promotionCodeDto.getCode())
                .expiryDate(promotionCodeDto.getExpireAfterDays() == null ? null :
                        Instant.now().plus(promotionCodeDto.getExpireAfterDays(), ChronoUnit.DAYS))
                .promotionValue(promotionCodeDto.getPromotionValue())
                .build();
        promotionCodeRepository.save(promotionCode);
        log.info("New promo code created: {}", promotionCode);
    }

    public void deleteCode(String id) {
        CommonUtils.checkRoleAuthorization(Authority.SUPER_USER);
        promotionCodeRepository.findOneByIdAndDeleteFlag(id, false)
                .map(code -> {
                    code.setDeleteFlag(true);
                    return code;
                }).map(promotionCodeRepository::save)
                .orElseThrow(() -> new EntityNotFoundException("promo code"));
        log.info("Promo code for id: {}, successfully marked as deleted.", id);
    }

    public PaginationDto getAllCodes(int pageNo, int pageSize, boolean includeDeleted, boolean includeExpired) {
        CommonUtils.checkRoleAuthorization(Authority.SUPER_USER);
        Pageable getSortedByCreatedDate = PageRequest.of(pageNo, pageSize, Sort.by("createdDate").descending());
        Page<PromotionCode> promotionCodePage = promotionCodeRepository
                .getAllByConditionalExpiredAndDeleted(includeDeleted, includeExpired, getSortedByCreatedDate);
        return CommonUtils.getPaginationResponse(promotionCodePage, this::entityToDto);
    }

    private PromotionCodeDto entityToDto(PromotionCode entity) {
        PromotionCodeDto dto = new PromotionCodeDto();
        dto.setId(entity.getId());
        dto.setType(entity.getType().toString());
        dto.setCode(entity.getCode());
        dto.setPromotionValue(entity.getPromotionValue());
        dto.setDeleted(entity.isDeleteFlag());
        dto.setExpired(Instant.now().isAfter(entity.getExpiryDate()));
        return dto;
    }

}