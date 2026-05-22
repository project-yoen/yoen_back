package com.yoen.yoen_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.common.security.JwtAuthenticationFilter;
import com.yoen.yoen_back.dto.etc.IdListRequest;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ImageController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ImageService imageService;

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    void upload_authenticatedUserAndImage_returnsImageUrl() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        MockMultipartFile imageFile = new MockMultipartFile("image", "image.jpg", "image/jpeg", "image".getBytes());
        Image image = imageEntity(10L, "image.jpg", "https://image.example/image.jpg");
        when(imageService.saveImage(eq(userDetails.user()), any())).thenReturn(image);

        mockMvc.perform(multipart("/image/create")
                        .file(imageFile)
                        .with(csrf())
                        .with(authentication(authenticationToken(userDetails))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("https://image.example/image.jpg"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(imageService).saveImage(eq(userDetails.user()), any());
    }

    @Test
    void uploadMultiple_authenticatedUserAndImages_returnsImageUrls() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        MockMultipartFile firstImage = new MockMultipartFile("images", "first.jpg", "image/jpeg", "first".getBytes());
        MockMultipartFile secondImage = new MockMultipartFile("images", "second.jpg", "image/jpeg", "second".getBytes());
        Image first = imageEntity(10L, "first.jpg", "https://image.example/first.jpg");
        Image second = imageEntity(11L, "second.jpg", "https://image.example/second.jpg");
        when(imageService.saveImages(eq(userDetails.user()), any())).thenReturn(List.of(first, second));

        mockMvc.perform(multipart("/image/multiple/create")
                        .file(firstImage)
                        .file(secondImage)
                        .with(csrf())
                        .with(authentication(authenticationToken(userDetails))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("https://image.example/first.jpg"))
                .andExpect(jsonPath("$.data[1]").value("https://image.example/second.jpg"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(imageService).saveImages(eq(userDetails.user()), any());
    }

    @Test
    void deleteSingle_existingImage_returnsDeletedMessage() throws Exception {
        when(imageService.deleteImage(10L)).thenReturn("https://image.example/image.jpg");

        mockMvc.perform(delete("/image/delete/{imageId}", 10L)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("The image has been successfully deleted: https://image.example/image.jpg"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(imageService).deleteImage(10L);
    }

    @Test
    void deleteMultiple_existingImages_returnsDeletedMessage() throws Exception {
        IdListRequest request = new IdListRequest(List.of(10L, 11L));
        when(imageService.deleteImages(List.of(10L, 11L))).thenReturn(List.of("https://image.example/first.jpg", "https://image.example/second.jpg"));

        mockMvc.perform(delete("/image/multiple/delete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("The image has been successfully deleted: https://image.example/first.jpg,https://image.example/second.jpg"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(imageService).deleteImages(List.of(10L, 11L));
    }

    private UsernamePasswordAuthenticationToken authenticationToken(CustomUserDetails userDetails) {
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private User userEntity() {
        return User.builder()
                .userId(1L)
                .email("alice@example.com")
                .password("password")
                .name("Alice")
                .nickname("alice")
                .gender(Gender.FEMALE)
                .birthday(LocalDate.of(2000, 1, 1))
                .build();
    }

    private Image imageEntity(Long imageId, String objectKey, String imageUrl) {
        return Image.builder()
                .imageId(imageId)
                .user(userEntity())
                .objectKey(objectKey)
                .imageUrl(imageUrl)
                .build();
    }
}
