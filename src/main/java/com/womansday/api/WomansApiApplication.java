package com.womansday.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WomansApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WomansApiApplication.class, args);
    }
}
