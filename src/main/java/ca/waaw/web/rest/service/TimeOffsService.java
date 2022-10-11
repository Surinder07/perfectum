package ca.waaw.web.rest.service;

import ca.waaw.repository.ShiftsRepository;
import ca.waaw.repository.TimeOffsRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TimeOffsService {

    private final Logger log = LogManager.getLogger(TimeOffsService.class);

    private final TimeOffsRepository timeOffsRepository;

    private final ShiftsRepository shiftsRepository;

}