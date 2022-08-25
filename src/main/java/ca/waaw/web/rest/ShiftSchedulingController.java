package ca.waaw.web.rest;

import ca.waaw.web.rest.service.ShiftSchedulingService;
import ca.waaw.web.rest.utils.APIConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = APIConstants.TagNames.shiftManagement, description = APIConstants.TagDescription.shiftManagement)
public class ShiftSchedulingController {

    private final ShiftSchedulingService shiftSchedulingService;

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.createShift)
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(APIConstants.ApiEndpoints.ShiftManagement.createShift)
    public void createShift() {
    }

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.updateShift)
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(APIConstants.ApiEndpoints.ShiftManagement.updateShift)
    public void updateShift() {
    }

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.deleteShift)
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping(APIConstants.ApiEndpoints.ShiftManagement.deleteShift)
    public void deleteShift() {
    }

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.assignShift)
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(APIConstants.ApiEndpoints.ShiftManagement.assignShift)
    public void assignShift() {
    }

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.claimShift)
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(APIConstants.ApiEndpoints.ShiftManagement.claimShift)
    public void claimShift() {
    }

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.createRecurringShift)
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(APIConstants.ApiEndpoints.ShiftManagement.createRecurringShift)
    public void createRecurringShift() {
    }

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.updateRecurringShift)
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(APIConstants.ApiEndpoints.ShiftManagement.updateRecurringShift)
    public void updateRecurringShift() {
    }

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.deleteRecurringShift)
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping(APIConstants.ApiEndpoints.ShiftManagement.deleteRecurringShift)
    public void deleteRecurringShift() {
    }

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.assignRecurringShift)
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(APIConstants.ApiEndpoints.ShiftManagement.assignRecurringShift)
    public void assignRecurringShift() {
    }

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.claimRecurringShift)
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(APIConstants.ApiEndpoints.ShiftManagement.claimRecurringShift)
    public void claimRecurringShift() {
    }

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.getAllShifts)
    @GetMapping(APIConstants.ApiEndpoints.ShiftManagement.getAllShifts)
    public ResponseEntity<Object> getAllShifts(@RequestParam(required = false) String locationId,
                                               @RequestParam(required = false) String location_role_id,
                                               @RequestParam String date, @RequestParam(required = false) String endDate,
                                               @PathVariable int pageNo, @PathVariable int pageSize) {
        return null;
    }

    @Operation(description = APIConstants.ApiDescription.ShiftManagement.getAllRecurringShifts)
    @GetMapping(APIConstants.ApiEndpoints.ShiftManagement.getAllRecurringShifts)
    public ResponseEntity<Object> getAllRecurringShifts(@RequestParam(required = false) String locationId,
                                                        @RequestParam(required = false) String location_role_id,
                                                        @PathVariable int pageNo, @PathVariable int pageSize) {
        return null;
    }

}