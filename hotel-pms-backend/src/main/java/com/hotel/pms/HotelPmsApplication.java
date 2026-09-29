package com.hotel.pms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotelPmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotelPmsApplication.class, args);
    }
}

