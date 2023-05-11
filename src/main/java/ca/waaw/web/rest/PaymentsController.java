package ca.waaw.web.rest;

import ca.waaw.dto.payments.CreditCardDto;
import ca.waaw.dto.PaginationDto;
import ca.waaw.dto.payments.PaymentsDto;
import ca.waaw.dto.userdtos.LoginResponseDto;
import ca.waaw.web.rest.service.PaymentsService;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerAuthenticated;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerBadRequest;
import ca.waaw.web.rest.utils.customannotations.swagger.SwaggerOk;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.payment-apis}")
public class PaymentsController {

    private final PaymentsService paymentsService;

    @SwaggerOk
    @SwaggerAuthenticated
    @Operation(description = "${api.description.payment-apis.createSetupIntent}")
    @GetMapping("${api.endpoints.payment-apis.createSetupIntent}")
    public ResponseEntity<Map<String, String>> createNewSetupIntent() {
        return ResponseEntity.ok(paymentsService.createNewSetupIntent());
    }

    @SwaggerOk
    @SwaggerAuthenticated
    @Operation(description = "${api.description.payment-apis.addNewCard}")
    @GetMapping("${api.endpoints.payment-apis.addNewCard}")
    public void addNewCard(@RequestParam String tokenId) {
        paymentsService.addNewCard(tokenId);
    }

    @SwaggerOk
    @SwaggerAuthenticated
    @Operation(description = "${api.description.payment-apis.updateDefaultCard}")
    @PutMapping("${api.endpoints.payment-apis.updateDefaultCard}")
    public void updateDefaultCard(@RequestParam String cardId) {
        paymentsService.updateDefaultCard(cardId);
    }

    @SwaggerOk
    @SwaggerAuthenticated
    @Operation(description = "${api.description.payment-apis.deleteCard}")
    @DeleteMapping("${api.endpoints.payment-apis.deleteCard}")
    public void deleteCard(@RequestParam String cardId) {
        paymentsService.deleteCard(cardId);
    }

    @SwaggerOk
    @SwaggerAuthenticated
    @Operation(description = "${api.description.payment-apis.getAllCards}")
    @GetMapping("${api.endpoints.payment-apis.getAllCards}")
    public ResponseEntity<List<CreditCardDto>> getAllCards() {
        return ResponseEntity.ok(paymentsService.getAllCards());
    }

    @SwaggerOk
    @SwaggerAuthenticated
    @Operation(description = "${api.description.payment-apis.getPendingPayment}")
    @GetMapping("${api.endpoints.payment-apis.getPendingPayment}")
    public ResponseEntity<PaymentsDto> getPendingPayment() {
        return ResponseEntity.ok(paymentsService.getPendingPayment());
    }

    @SwaggerOk
    @SwaggerAuthenticated
    @Operation(description = "${api.description.payment-apis.createPaymentIntent}")
    @GetMapping("${api.endpoints.payment-apis.createPaymentIntent}")
    public ResponseEntity<Map<String, String>> createNewPaymentIntent(@RequestParam String paymentId) {
        return ResponseEntity.ok(paymentsService.createNewPaymentIntent(paymentId));
    }

    @SwaggerBadRequest
    @SwaggerAuthenticated
    @Operation(description = "${api.description.payment-apis.getPayments}")
    @GetMapping("${api.endpoints.payment-apis.getPayments}")
    @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(
            schema = @Schema(implementation = PaymentsDto.class)))},
            description = "${api.swagger.schema-description.pagination}")
    public ResponseEntity<PaginationDto> getAllPayments(@PathVariable int pageNo,
                                                        @PathVariable int pageSize) {
        try {
            return ResponseEntity.ok(paymentsService.getFullPaymentHistory(pageNo, pageSize, null, null, null));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SwaggerOk
    @SwaggerAuthenticated
    @Operation(description = "${api.description.payment-apis.getPaymentById}")
    @GetMapping("${api.endpoints.payment-apis.getPaymentById}")
    public ResponseEntity<PaymentsDto> getPaymentHistoryById(@RequestParam String paymentId) {
        return ResponseEntity.ok(paymentsService.getPaymentHistoryById(paymentId));
    }

    @SwaggerOk
    @SwaggerAuthenticated
    @Operation(description = "${api.description.payment-apis.confirmPayment}")
    @GetMapping("${api.endpoints.payment-apis.confirmPayment}")
    public ResponseEntity<LoginResponseDto> confirmPayment(@RequestParam String paymentId, @RequestParam boolean success) {
        try {
            return ResponseEntity.ok(paymentsService.confirmPayment(paymentId, success));
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}