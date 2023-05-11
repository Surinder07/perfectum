# WAAW-API

### Requirements

- JAVA 11
- MAVEN
- MySql
- Any Java IDE

> __Warning__
> 
> If you are setting up java and maven for the first time, make sure you have your java and maven environment variables set properly on your system.

### Database

Have MySql Workbench installed on the system and change your database username and password in the `application-dev.yml` file, it will automatically create the database when you start the application.

While running the application for the first time, set `spring.liquibase.enabled=true` in your `application.yml` and tables will be automatically created by `liquibase` and sql triggers will be executed by custom method written in class `ApplicationStartupSqlService`, which also creates-
- An application **super-user**.
- A Dummy organization, location and two roles (admin and non admin).
- Some dummy users with roles **ADMIN**, **MANAGER**, and **EMPLOYEE**.
- Four Promo Codes for application (**WAAW01**, **WAAW10**, **WAAW20**, and **WAAW30**) providing **1, 10, 20 ,and 30** days of ***trial period*** respectively.

> __Warning__
> 
> Do not make any changes to the `changelog files` or the application will start throwing error for mismatch in changelogs

### Resources

- **Swagger Url** : http://localhost:8080/swagger-ui.html
- **Swagger Url (deployed on staging)** : https://staging-api.waaw.ca/swagger-ui.html

> __Note__
> 
> All details about websockets are available in swagger doc description.


### Developer Notes

- All Api Endpoints and swagger descriptions can be found in api-info directory under resources.
- All error messages, Api Response and notification data can be found in the messages.properties bundle, mapped through `ca/waaw/web/rest/errors/ErrorMessageKeys`, `ca/waaw/web/rest/utils/ApiResponseMessageKeys` and `ca/waaw/web/rest/utils/MessageConstants` respectively.
- All custom or native queries used in our repositories can be found in `ca/waaw/web/rest/utils/jpasqlqueries`.
- Api endpoint `/test/**` is left open. So any testing can be done using this endpoint without jwt token.
- Logs are configured to be created in a directory named **waaw/springLogs** in your base directory for dev profile, and **/home/site/wwwroot/springLogs** in base directory for other profiles to keep directory structure relevant for azure.
- All empty strings in request body or request param are converted to null values using two classes:
  - For request params, in `ca/waaw/web/rest/errors/RestExceptionHandler` (controller advice class) we have added a method initBinder.
  - For request body we have added a class `GlobalDeserializeConfigurer`.