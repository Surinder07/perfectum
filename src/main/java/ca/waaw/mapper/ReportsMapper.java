package ca.waaw.mapper;

import ca.waaw.domain.joined.UserReports;

import java.util.List;

public class ReportsMapper {

    /**
     * @param reports list of report entity
     * @return list of an object array containing attendance info to be written on a file.
     */
    public static List<Object[]> getAttendanceReport(List<UserReports> reports) {
        // TODO include header in the first object
        return null;
    }

    /**
     * @param reports list of report entity
     * @return list of an object array containing payroll info to be written on a file.
     */
    public static List<Object[]> getPayrollReport(List<UserReports> reports) {
        // TODO include header in the first object
        return null;
    }

}
