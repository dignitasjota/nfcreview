package com.reviewtap.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor acotado para registrar interacciones fuera del hilo de la petición de redirect.
 * Si la cola se llena, {@link ThreadPoolExecutor.CallerRunsPolicy} degrada a ejecución síncrona:
 * nunca se descarta una interacción silenciosamente.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String INTERACTION_EXECUTOR = "interactionExecutor";

    /** {@code app.interactions.async=false} sustituye el pool por ejecución síncrona (tests). */
    @Bean(INTERACTION_EXECUTOR)
    @ConditionalOnProperty(prefix = "app.interactions", name = "async", havingValue = "true", matchIfMissing = true)
    TaskExecutor interactionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("interaction-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(2000);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @Bean(INTERACTION_EXECUTOR)
    @ConditionalOnProperty(prefix = "app.interactions", name = "async", havingValue = "false")
    TaskExecutor syncInteractionExecutor() {
        return new SyncTaskExecutor();
    }
}
