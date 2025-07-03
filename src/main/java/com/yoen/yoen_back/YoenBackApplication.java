package com.yoen.yoen_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
public class YoenBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(YoenBackApplication.class, args);
	}

}
