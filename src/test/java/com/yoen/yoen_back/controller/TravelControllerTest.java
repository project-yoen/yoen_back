package com.yoen.yoen_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.common.security.JwtAuthenticationFilter;
import com.yoen.yoen_back.dto.travel.TravelNicknameUpdateDto;
import com.yoen.yoen_back.dto.travel.TravelProfileImageDto;
import com.yoen.yoen_back.dto.travel.TravelRequestDto;
import com.yoen.yoen_back.dto.travel.TravelResponseDto;
import com.yoen.yoen_back.dto.travel.TravelUserDto;
import com.yoen.yoen_back.dto.travel.TravelUserResponseDto;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.TravelService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TravelController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc
class TravelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TravelService travelService;

    @MockitoBean
    private AuthService authService;

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
    void getAllTravelByUser_authenticatedUser_returnsTravelList() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        when(travelService.getAllTravelByUser(userDetails.user())).thenReturn(List.of(travelResponse()));

        mockMvc.perform(get("/travel")
                        .with(authentication(authenticationToken(userDetails))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].travelId").value(10L))
                .andExpect(jsonPath("$.data[0].travelName").value("Tokyo Trip"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(travelService).getAllTravelByUser(userDetails.user());
    }

    @Test
    void travel_returnsAllTravels() throws Exception {
        Travel travel = travelEntity();
        when(travelService.getAllTravels()).thenReturn(List.of(travel));

        mockMvc.perform(get("/travel/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].travelId").value(10L))
                .andExpect(jsonPath("$.data[0].travelName").value("Tokyo Trip"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(travelService).getAllTravels();
    }

    @Test
    void setTravel_authenticatedUser_createsTravel() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        TravelRequestDto request = travelRequest();
        when(travelService.createTravel(userDetails.user(), request)).thenReturn(travelResponse());

        mockMvc.perform(post("/travel/create")
                        .with(csrf())
                        .with(authentication(authenticationToken(userDetails)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.travelId").value(10L))
                .andExpect(jsonPath("$.data.travelName").value("Tokyo Trip"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(travelService).createTravel(userDetails.user(), request);
    }

    @Test
    void deleteTravel_writerUser_deletesTravel() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        TravelUser travelUser = travelUserEntity(Role.WRITER);
        when(authService.checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.WRITER))).thenReturn(travelUser);

        mockMvc.perform(delete("/travel/delete")
                        .with(csrf())
                        .with(authentication(authenticationToken(userDetails)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isString())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(authService).checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.WRITER));
        verify(travelService).deleteTravel(travelUser.getTravel());
    }

    @Test
    void getTravelUser_authorizedUser_returnsTravelUserDto() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        TravelUser travelUser = travelUserEntity(Role.READER);
        TravelUserDto response = new TravelUserDto(100L, 1L, 10L, Role.READER, "alice-trip");
        when(authService.checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER))).thenReturn(travelUser);
        when(travelService.getTravelUser(userDetails.user(), travelUser.getTravel())).thenReturn(response);

        mockMvc.perform(get("/travel/traveluser")
                        .param("travelId", "10")
                        .with(authentication(authenticationToken(userDetails))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.travelUserId").value(100L))
                .andExpect(jsonPath("$.data.travelNickname").value("alice-trip"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(authService).checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER));
        verify(travelService).getTravelUser(userDetails.user(), travelUser.getTravel());
    }

    @Test
    void updateTravelNickname_authorizedUser_updatesNickname() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        TravelNicknameUpdateDto request = new TravelNicknameUpdateDto(100L, 10L, "new-nickname");
        TravelUser travelUser = travelUserEntity(Role.READER);
        when(authService.checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER))).thenReturn(travelUser);

        mockMvc.perform(post("/travel/traveluser/nickname")
                        .with(csrf())
                        .with(authentication(authenticationToken(userDetails)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isString())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(authService).checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER));
        verify(travelService).updateTravelUserNickname(request);
    }

    @Test
    void getAllTravelUsers_authorizedUser_returnsTravelUsers() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        TravelUser travelUser = travelUserEntity(Role.WRITER);
        TravelUserDto response = new TravelUserDto(100L, 1L, 10L, Role.WRITER, "alice-trip");
        when(authService.checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER))).thenReturn(travelUser);
        when(travelService.getAllTravelUser(travelUser.getTravel())).thenReturn(List.of(response));

        mockMvc.perform(get("/travel/traveluser/all")
                        .param("travelId", "10")
                        .with(authentication(authenticationToken(userDetails))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].travelUserId").value(100L))
                .andExpect(jsonPath("$[0].travelNickname").value("alice-trip"));

        verify(authService).checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER));
        verify(travelService).getAllTravelUser(travelUser.getTravel());
    }

    @Test
    void getDetailTravelUsers_authorizedUser_returnsUserDetails() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        TravelUser travelUser = travelUserEntity(Role.WRITER);
        TravelUserResponseDto response = new TravelUserResponseDto(100L, "Alice", "alice-trip", Gender.FEMALE, LocalDate.of(2000, 1, 1), "");
        when(authService.checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER))).thenReturn(travelUser);
        when(travelService.getDetailTravelUser(travelUser.getTravel())).thenReturn(List.of(response));

        mockMvc.perform(get("/travel/userdetail")
                        .param("travelId", "10")
                        .with(authentication(authenticationToken(userDetails))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].travelUserId").value(100L))
                .andExpect(jsonPath("$.data[0].nickName").value("Alice"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(authService).checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER));
        verify(travelService).getDetailTravelUser(travelUser.getTravel());
    }

    @Test
    void leaveTravel_authorizedUser_leavesTravel() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        TravelUser travelUser = travelUserEntity(Role.READER);
        when(authService.checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER))).thenReturn(travelUser);

        mockMvc.perform(post("/travel/leave/{travelId}", 10L)
                        .with(csrf())
                        .with(authentication(authenticationToken(userDetails))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isString())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(authService).checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER));
        verify(travelService).leaveTravel(travelUser);
    }

    @Test
    void updateTravelProfileImage_authorizedUser_updatesProfileImage() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        TravelUser travelUser = travelUserEntity(Role.READER);
        TravelProfileImageDto request = new TravelProfileImageDto(10L, -1L);
        MockMultipartFile dtoPart = new MockMultipartFile(
                "dto",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );
        MockMultipartFile imagePart = new MockMultipartFile("image", "travel.jpg", "image/jpeg", "image".getBytes());
        when(authService.checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER))).thenReturn(travelUser);

        mockMvc.perform(multipart("/travel/image/update")
                        .file(dtoPart)
                        .file(imagePart)
                        .with(csrf())
                        .with(authentication(authenticationToken(userDetails))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isString())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(authService).checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER));
        verify(travelService).updateTravelProfileImage(eq(userDetails.user()), eq(travelUser.getTravel()), eq(request), any());
    }

    @Test
    void getTravelDetail_authorizedUser_returnsTravelDetail() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        when(authService.checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER))).thenReturn(travelUserEntity(Role.READER));
        when(travelService.getTravelDetail(10L)).thenReturn(travelResponse());

        mockMvc.perform(get("/travel/detail")
                        .param("travelId", "10")
                        .with(authentication(authenticationToken(userDetails))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.travelId").value(10L))
                .andExpect(jsonPath("$.data.travelName").value("Tokyo Trip"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(authService).checkTravelUserRoleByTravel(userDetails.user(), 10L, List.of(Role.READER, Role.WRITER));
        verify(travelService).getTravelDetail(10L);
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

    private Travel travelEntity() {
        return Travel.builder()
                .travelId(10L)
                .travelName("Tokyo Trip")
                .numOfPeople(3L)
                .numOfJoinedPeople(1L)
                .nation(Nation.JAPAN)
                .sharedFund(0L)
                .startDate(LocalDate.of(2025, 7, 1))
                .endDate(LocalDate.of(2025, 7, 3))
                .build();
    }

    private TravelUser travelUserEntity(Role role) {
        return TravelUser.builder()
                .travelUserId(100L)
                .travel(travelEntity())
                .user(userEntity())
                .role(role)
                .travelNickname("alice-trip")
                .build();
    }

    private TravelRequestDto travelRequest() {
        return new TravelRequestDto(
                null,
                "Tokyo Trip",
                3L,
                Nation.JAPAN,
                "2025-07-01",
                "2025-07-03",
                List.of(1L, 2L)
        );
    }

    private TravelResponseDto travelResponse() {
        return new TravelResponseDto(
                10L,
                3L,
                1L,
                Nation.JAPAN,
                0L,
                "Tokyo Trip",
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2025, 7, 3),
                ""
        );
    }
}
