package com.devott;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DevottApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevottApiApplication.class, args);
	}

}
