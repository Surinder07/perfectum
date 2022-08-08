package ca.waaw;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "${swagger.title}", version = "${swagger.title}", description = "${swagger.description}"))
@SecurityScheme(name = "jwt", scheme = "bearer", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class WaawApplication implements CommandLineRunner {

    private final Logger log = LogManager.getLogger(WaawApplication.class);

    private final Environment env;

    public WaawApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        System.out.println("JAVA HOME PATH: " + System.getenv("JAVA_HOME"));
        SpringApplication.run(WaawApplication.class, args);
    }

    @Override
    public void run(String... args) {
        logApplicationStartup();
    }

    private void logApplicationStartup() {
        String protocol = Optional.ofNullable(env.getProperty("server.ssl.key-store")).map(key -> "https").orElse("http");
        String serverPort = env.getProperty("server.port");
        String contextPath = Optional.ofNullable(env.getProperty("server.servlet.context-path")).filter(StringUtils::isNotBlank).orElse("/");
        String hostAddress;
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            hostAddress = "localhost";
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }
        log.info("\n----------------------------------------------------------\n\t" +
                        "Application '{}' is running! Access URLs:\n\t" + "Local: \t\t{}://localhost:{}{}\n\t"
                        + "External: \t{}://{}:{}{}\n\t" + "Profile(s): \t{}\n----------------------------------------------------------",
                env.getProperty("application.title"), protocol, serverPort, contextPath, protocol, hostAddress, serverPort, contextPath,
                env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles());
        System.out.println("Application is running on port " + env.getProperty("server.port"));
    }

}
