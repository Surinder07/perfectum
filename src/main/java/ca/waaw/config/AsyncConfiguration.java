package ca.waaw.config;

import ca.waaw.config.applicationconfig.AppAsyncConfig;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SuppressWarnings("unused")
@EnableAsync
@Configuration
@AllArgsConstructor
public class AsyncConfiguration {

    private final AppAsyncConfig appAsyncConfig;

    private final static Logger log = LogManager.getLogger(AsyncConfiguration.class);

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(appAsyncConfig.getCorePoolSize());
        executor.setMaxPoolSize(appAsyncConfig.getMaxPoolSize());
        executor.setQueueCapacity(appAsyncConfig.getQueueCapacity());
        executor.setThreadNamePrefix(appAsyncConfig.getThreadNamePrefix());
        executor.initialize();
        log.info("Async Executor initialized successfully: core pool size {}, max pool size {}, " +
                "queue capacity {}, thread name prefix {}", executor.getCorePoolSize(), executor.getMaxPoolSize(),
                executor.getQueueCapacity(), executor.getThreadNamePrefix());
        return executor;
    }

}
   
