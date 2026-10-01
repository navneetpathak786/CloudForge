package com.cloudforge.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkerAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerAgentApplication.class, args);
    }
}
