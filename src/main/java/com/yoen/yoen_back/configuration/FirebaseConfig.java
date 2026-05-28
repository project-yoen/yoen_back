package com.yoen.yoen_back.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Configuration
public class FirebaseConfig {

    private final Environment env;

    public FirebaseConfig(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void init() {
        if (FirebaseApp.getApps().isEmpty()) {
            String resourcePath = env.getProperty("firebase.config.path");
            String storageBucket = env.getProperty("firebase.storage.bucket");

            try (InputStream serviceAccount = openFirebaseConfig(resourcePath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setStorageBucket(storageBucket)
                        .build();

                FirebaseApp.initializeApp(options);
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new RuntimeException("Firebase initialization failed", e);
            }
        }
    }
    private InputStream openFirebaseConfig(String resourcePath) throws Exception {
        InputStream classpathResource = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (classpathResource != null) {
            return classpathResource;
        }

        Path filePath = Path.of(resourcePath);
        if (Files.exists(filePath)) {
            return Files.newInputStream(filePath);
        }

        throw new FileNotFoundException("Firebase config not found: " + resourcePath);
    }

    @Bean
    public Bucket bucket() {
        return StorageClient.getInstance().bucket(); // 이걸 Bean으로만 등록
    }

}
