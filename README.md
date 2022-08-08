# WAAW-API

### Requirements

- JAVA 11
- MAVEN
- MySql

### Database

Have MySql Workbench installed on the system and change your database info in the application-dev.yml file.

While running the application for the first time, set `spring.liquibase.enabled=true` in your `application.yml` and tables will be automatically created by `liquibase`, though you have to add the triggers into your sql manually, triggers can be found under `resources/db/sqltriggers` under file name same as the table name. Make sure to disable liquibase after setting up you database.

### Resources

- **Swagger Url** : http://localhost:8080/swagger-ui.html