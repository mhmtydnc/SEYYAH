package com.seyyah;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeyyahApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeyyahApplication.class, args);
	}

}
