package ca.waaw;

import ca.waaw.dto.locationandroledtos.NewLocationDto;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/test")
@AllArgsConstructor
public class ApiTestingController {

    @PostMapping("/1")
    public NewLocationDto test1(@Valid @RequestBody NewLocationDto dto) {
        return dto;
    }

}
