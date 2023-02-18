package ca.waaw;

import ca.waaw.filehandler.utils.PojoToFileUtils;
import ca.waaw.notification.EmailService;
import ca.waaw.notification.SMSService;
import ca.waaw.repository.RequestsRepository;
import ca.waaw.repository.UserRepository;
import ca.waaw.repository.joined.BatchDetailsRepository;
import ca.waaw.repository.joined.ShiftDetailsRepository;
import ca.waaw.storage.AzureStorage;
import ca.waaw.web.rest.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/test")
public class Test {

    @Autowired
    Environment env;

    @Autowired
    SMSService service;

    @Autowired
    EmailService emailService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BatchDetailsRepository batchDetailsRepository;

    @Autowired
    ShiftDetailsRepository shiftDetailsRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    RequestsRepository requestsRepository;

    @GetMapping("/1")
    public void test() {
        System.out.println(passwordEncoder.encode("Empl123$"));
    }

    @GetMapping("/3")
    public Object test3(HttpServletRequest request) {
        try {
            Pageable getSortedByCreatedDate = PageRequest.of(0, 10, Sort.by("createdDate").descending());
//            Page<SingularShiftDetails> page = repository.searchAndFilterShiftsDate(null, null,
//                    "01f2c3d1-e987-4095-b4db-7c94fd977366", null, null, getSortedByCreatedDate);
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

    @GetMapping("/dummy-shift")
    public void createDummyShifts() {
        try {

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/6")
    public Object test6() throws IOException {
        try {
        return requestsRepository.getOverlappingForDates(Instant.now().plus(1, ChronoUnit.DAYS),
                Instant.now().plus(5, ChronoUnit.DAYS), false);
    } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}