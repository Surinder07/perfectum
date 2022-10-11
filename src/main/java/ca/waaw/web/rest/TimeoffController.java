package ca.waaw.web.rest;

import ca.waaw.web.rest.service.TimeOffsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("unused")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
@Tag(name = "${api.swagger.groups.timeoff}")
public class TimeoffController {

    private final TimeOffsService timeOffsService;

    // New timeoff request (admin can add directly, or employee can request) If it is for existing shift,
    // create an unassigned shift for it and send notification to admin

    // See all timeoff requests with extra option to see allowed ones for admin

    // Accept or reject the request for admin

    // Edit timeoff request

    // Delete the timeoff request

}