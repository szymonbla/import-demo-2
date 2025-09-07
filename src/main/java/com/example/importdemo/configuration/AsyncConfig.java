package com.example.importdemo.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);   // Liczba wątków trzymanych w puli
        executor.setMaxPoolSize(10);   // Maksymalna liczba wątków
        executor.setQueueCapacity(25); // Pojemność kolejki na zadania
        executor.setThreadNamePrefix("Import-");
        executor.initialize();
        return executor;
    }
}