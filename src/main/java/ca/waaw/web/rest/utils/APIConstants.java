package ca.waaw.web.rest.utils;

/**
 * All Api related constants like API endpoints, swagger descriptions, etc.
 */
public final class APIConstants {

    public static class TagNames {
        public static final String auth = "1. Auth";
        public static final String user = "2. User";
        public static final String notification = "3. Notifications";
        public static final String locationAndRole = "4. Location and Location Role";
        public static final String shiftScheduling = "5. Shift Scheduling";
    }

    public static class TagDescription {
        public static final String auth = "Authentication API(s)";
        public static final String user = "All user registration and management API(s)";
        public static final String notification = "All notification rest API(s)";
        public static final String locationAndRole = "All location and location role related API(s)";
        public static final String shiftScheduling = "All shift and recurring shifts related API(s)";
    }

    public static class ApiEndpoints {

        public static class Auth {
            public static final String authentication = "/v1/unAuth/authenticate";
        }

        public static class User {
            public static final String checkUsername = "/v1/unAuth/checkUserNameExistence";
            public static final String registerOrganization = "/v1/unAuth/registerAdmin";
            public static final String registerUser = "/v1/unAuth/registerUser";
            public static final String resetPasswordInit = "/v1/unAuth/resetPassword/init";
            public static final String resetPasswordFinish = "/v1/unAuth/resetPassword/finish";
            public static final String activateAccount = "/v1/unAuth/activateAccount";
            public static final String acceptInvitation = "/v1/unAuth/acceptInvitation";
            public static final String updateUser = "/v1/updateUser";
            public static final String updatePassword = "v1/updatePassword";
            public static final String updateProfileImage = "/v1/updateProfileImage";
            public static final String sendInvite = "/v1/sendInvite";
            public static final String getUserDetails = "/v1/getAccount";
            public static final String getAllUsers = "/v1/users/getAll";
        }

        public static class Notification {
            public static final String getAllNotification = "/v1/notifications/getAll/{pageNo}/{pageSize}";
            public static final String markNotificationAsRead = "/v1/notifications/markAsRead";
            public static final String markAllNotificationAsRead = "/v1/notifications/markAllAsRead";
            public static final String deleteNotification = "/v1/notifications/delete";
        }

        public static class LocationAndRole {
            public static final String getLocation = "/v1/location/get";
            public static final String addLocation = "/v1/location/save";
            public static final String deleteLocation = "/v1/location/delete";
            public static final String addLocationRole = "/v1/location/role/save";
            public static final String deleteLocationRole = "/v1/location/role/delete";
            public static final String getLocationRole = "/v1/location/role/get";
            public static final String updateLocationRole = "/v1/location/role/update";
        }
    }

    public static class ApiDescription {

        public static class Auth {
            public static final String authentication = "Authenticate login password to get a jwt token";
        }

        public static class User {
            public static final String checkUsername = "Will Return Success only if given username is available";
            public static final String registerUser = "Register a new user (by email invite only)";
            public static final String registerOrganization = "Register a new user (admin) with an organization";
            public static final String updateUser = "Update logged in user details";
            public static final String updatePassword = "Update current password using the old password";
            public static final String resetPasswordInit = "Initialize a password reset request and get email to reset password";
            public static final String resetPasswordFinish = "Finish password reset request with key received on email";
            public static final String updateProfileImage = "Update profile image for logged in user. (Not operational right now)";
            public static final String sendInvite = "Invite new users to join logged in admins organization";
            public static final String getUserDetails = "Get Logged in user's account details";
            public static final String getAllUsers = "Get all Employees and Admins under logged-in user";
        }

        public static class Notification {
            public static final String getAllNotification = "Get all notifications, Page numbers start with 0";
            public static final String markNotificationAsRead = "Mark notification with given id as read";
            public static final String markAllNotificationAsRead = "Mark all notifications as read";
            public static final String deleteNotification = "Delete a notification";
        }

        public static class LocationAndRole {
            public static final String getLocation = "Api to get information location and roles under them.";
            public static final String addLocation = "Adds a new location under logged in admins organization";
            public static final String deleteLocation = "Deletes the location with given Id and suspends the account of related users";
            public static final String addLocationRole = "Adds a new location role under logged in admins organization";
            public static final String deleteLocationRole = "Deletes the location role with given Id and suspends the account of related users";
            public static final String getLocationRole = "Api to get information about location roles.";
            public static final String updateLocationRole = "Update a location role under logged in admins organization";
        }
    }

    public static class ErrorDescription {
        public static final String trialOver = "If role is ADMIN redirect to payment page with error message or else just show the error.";
        public static final String authentication = "Authentication Failure";
    }

    public static class SchemaDescription {
        public static final String pagination = "Response will contain, </li><li>Total number of pages(totalPages)</li><li>Number of entries(totalEntries)</li><li>List of response(data)</li>";
        public static final String getLocation = "<li>For Global Admin, a list will be returned.</li><li>For Location Manager a location with list of roles will be returned.</li><li>For Employee single location and single role will be returned.</li>";
        public static final String getLocationRole = "<li>For Global and Location Admin, users will be returned.</li><li>For Employee only role info will be returned.";
    }

}
