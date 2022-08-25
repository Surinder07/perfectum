package ca.waaw.web.rest;

import ca.waaw.web.rest.service.ShiftSchedulingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "Shift Scheduling", description = "Shift scheduling Apis")
public class ShiftSchedulingController {

    private final ShiftSchedulingService shiftSchedulingService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/v1/shifts/create")
    public void createShift() {}

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/v1/shifts/update")
    public void updateShift() {}

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/v1/shifts/delete")
    public void deleteShift() {}

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/v1/shifts/assign")
    public void assignShift() {}

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/v1/shifts/claim")
    public void claimShift() {}

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/v1/recurringShifts/create")
    public void createRecurringShift() {}

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/v1/recurringShifts/update")
    public void updateRecurringShift() {}

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/v1/recurringShifts/delete")
    public void deleteRecurringShift() {}

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/v1/recurringShifts/assign")
    public void assignRecurringShift() {}

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/v1/recurringShifts/claim")
    public void claimRecurringShift() {}

    @GetMapping("/v1/shifts/getAll/{pageNo}/{pageSize}")
    public ResponseEntity<Object> getAllShifts(@RequestParam(required = false) String locationId,
                                               @RequestParam(required = false) String location_role_id) {
        return null;
    }

    @GetMapping("/v1/recurringShifts/getAll/{pageNo}/{pageSize}")
    public ResponseEntity<Object> getAllRecurringShifts() {
        return null;
    }

}