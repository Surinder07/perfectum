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


### Project Structure

- All Api Endpoints and swagger descriptions can be found in apiinfo.yml file.
- All error messages can be found in the messages.properties bundle.