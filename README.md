# WAAW-API

### Requirements

- JAVA 11
- MAVEN
- MySql

### Database

Have MySql Workbench installed on the system and change your database username and password in the `application-dev.yml` file, it will automatically create the database when you start the application.

While running the application for the first time, set `spring.liquibase.enabled=true` in your `application.yml` and tables will be automatically created by `liquibase`and sql triggers will be executed by custom method written in class `ApplicationStartupSqlService`, which also creates an application **super-user**. Make sure to disable liquibase after setting up you database by setting `spring.liquibase.enabled=false` in your `application.yml`.

### Resources

- **Swagger Url** : http://localhost:8080/swagger-ui.html

> All details about websockets is available in swagger doc description.