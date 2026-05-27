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

    // ImageController의 multipart 업로드와 삭제 요청 매핑을 서비스 mock으로 검증한다.
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ImageService imageService;

    @TestConfiguration
    static class TestSecurityConfig {

        // 이미지 컨트롤러 테스트에서는 JWT 검증 대신 principal을 직접 주입한다.
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
        // 단일 이미지 multipart 요청이 ImageService.saveImage로 전달되는지 검증한다.
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
        // 여러 이미지 multipart 요청이 ImageService.saveImages로 전달되는지 검증한다.
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
        // 이미지 ID path variable이 단일 삭제 서비스로 전달되는지 검증한다.
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
        // 이미지 ID 목록 request body가 여러 이미지 삭제 서비스로 전달되는지 검증한다.
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
        // @AuthenticationPrincipal에 CustomUserDetails를 주입하기 위한 인증 fixture.
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private User userEntity() {
        // 이미지 업로드 요청의 인증 사용자 fixture.
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
        // ImageService mock이 반환할 이미지 fixture.
        return Image.builder()
                .imageId(imageId)
                .user(userEntity())
                .objectKey(objectKey)
                .imageUrl(imageUrl)
                .build();
    }
}
