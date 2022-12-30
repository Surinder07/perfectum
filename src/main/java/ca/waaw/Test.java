package ca.waaw;

import ca.waaw.domain.joined.BatchDetails;
import ca.waaw.domain.joined.ShiftDetails;
import ca.waaw.dto.userdtos.UpdateUserDto;
import ca.waaw.filehandler.utils.PojoToFileUtils;
import ca.waaw.repository.UserRepository;
import ca.waaw.repository.joined.BatchDetailsRepository;
import ca.waaw.repository.joined.ShiftDetailsRepository;
import ca.waaw.web.rest.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/test")
public class Test {

    @Autowired
    Environment env;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BatchDetailsRepository batchDetailsRepository;

    @Autowired
    ShiftDetailsRepository shiftDetailsRepository;

    @PostMapping("/1")
    public void test(@RequestBody @Valid UpdateUserDto dto) {
        System.out.println(dto);
    }

    @GetMapping("/3")
    public Object test3(HttpServletRequest request) {
        try {
            Pageable getSortedByCreatedDate = PageRequest.of(0, 10, Sort.by("createdDate").descending());
//            Page<BatchDetails> details = batchDetailsRepository.searchAndFilterShifts(null, null, null, null,
//                    null, getSortedByCreatedDate);
//            List<String> batchIds = details.getContent().stream()
//                    .map(BatchDetails::getId).collect(Collectors.toList());
//            List<ShiftDetails> shifts = shiftDetailsRepository.searchAndFilterShifts(null, null, null, null, null, true, batchIds);
            Map<String, Object> res = new HashMap<>();
//            res.put("batch", details);
//            res.put("shifts", shifts);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/2")
    public ResponseEntity<Resource> test2() {
        Object[] obj1 = new Object[]{"head1", "head2", "head3", "head4"};
        Object[] obj2 = new Object[]{"test", "test", false, 2};
        List<Object[]> list = new ArrayList<>();
        list.add(obj1);
        list.add(obj2);
        ByteArrayResource resource = PojoToFileUtils.convertObjectToListOfWritableObject(list, "test", "csv");
        return CommonUtils.byteArrayResourceToResponse(resource, "test.csv");
    }

}