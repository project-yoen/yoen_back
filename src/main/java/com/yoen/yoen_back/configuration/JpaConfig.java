package com.yoen.yoen_back.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"com.yoen.yoen_back.repository.jpa"}) // 또는 정확하게 JPA 전용 패키지로
public class JpaConfig {
}
