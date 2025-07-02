package com.yoen.yoen_back.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.InputStream;

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

            log.info("=== Firebase Config Debug ===");
            log.info("firebase.config.path: {}", resourcePath);
            log.info("firebase.config.bucket: {}", storageBucket);
            log.info("==============================");

            try (InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                assert serviceAccount != null;
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setStorageBucket(storageBucket)
                        .build();

                FirebaseApp.initializeApp(options);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Firebase initialization failed", e);
            }
        }
    }
}
