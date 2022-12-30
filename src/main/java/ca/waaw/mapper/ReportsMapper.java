package ca.waaw.mapper;

import ca.waaw.domain.joined.UserReports;

import java.util.ArrayList;
import java.util.List;

public class ReportsMapper {

    /**
     * @param reports list of report entity
     * @return list of an object array containing attendance info to be written on a file.
     */
    public static List<Object[]> getAttendanceReport(List<UserReports> reports) {
        List<Object[]> response = new ArrayList<>();
        String[] headers = new String[]{"Employee/Contractor Name", "Email Id", "Employee Id", "WAAW Id", "Full Time Employee",
                "Location", "Role", "Shifts Assigned in Hours", "Statutory Holiday in Hours", "Vacation in Hours",
                "Sick Leaves in Hours", "Hours Worked", "Hourly Rate"};
        response.add(headers);
        reports.forEach(report -> {
            //TODO map data to Object[] and add to response
        });
        return response;
    }

    /**
     * @param reports list of report entity
     * @return list of an object array containing payroll info to be written on a file.
     */
    public static List<Object[]> getPayrollReport(List<UserReports> reports) {
        List<Object[]> response = new ArrayList<>();
        String[] headers = new String[]{"Employee/Contractor Name", "Email Id", "Employee Id", "WAAW Id", "Full Time Employee",
                "Location", "Role", "Shift Assigned", "Working Start Time", "Working End Time", "Working Timezone",
                "Working Start Time in UTC", "Working End Time in UTC"};
        response.add(headers);
        reports.forEach(report -> {
            //TODO map data to Object[] and add to response
        });
        return response;
    }

}
