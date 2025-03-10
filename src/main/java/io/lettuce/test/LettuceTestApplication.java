package io.lettuce.test;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LettuceTestApplication implements ApplicationRunner {

    private final LettuceWorkloadRunner runner;

    public LettuceTestApplication(LettuceWorkloadRunner runner) {
        this.runner = runner;
    }

    public static void main(String[] args) {
        SpringApplication.run(LettuceTestApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        runner.run();
    }

}
