package com.yoen.yoen_back.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles("test")
public abstract class RedisIntegrationTestSupport {

    private static final int REDIS_PORT = 6379;

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", RedisIntegrationTestSupport::redisHost);
        registry.add("spring.data.redis.port", RedisIntegrationTestSupport::redisPort);
    }

    protected static String redisHost() {
        startRedisIfNecessary();
        return REDIS.getHost();
    }

    protected static int redisPort() {
        startRedisIfNecessary();
        return REDIS.getMappedPort(REDIS_PORT);
    }

    private static void startRedisIfNecessary() {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }

    @TestConfiguration
    public static class RedisTestConfig {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory(redisHost(), redisPort());
        }

        @Bean
        RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
            RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory);

            StringRedisSerializer serializer = new StringRedisSerializer();
            redisTemplate.setKeySerializer(serializer);
            redisTemplate.setValueSerializer(serializer);
            redisTemplate.setHashKeySerializer(serializer);
            redisTemplate.setHashValueSerializer(serializer);
            redisTemplate.afterPropertiesSet();

            return redisTemplate;
        }
    }
}
