package ca.waaw.web.rest.service;

import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ShiftSchedulingService {

    private final Logger log = LogManager.getLogger(ShiftSchedulingService.class);

}