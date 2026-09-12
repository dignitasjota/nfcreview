package com.reviewtap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReviewTapApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewTapApplication.class, args);
    }
}
