package com.yoen.yoen_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoen.yoen_back.dto.etc.CategoryRequestDto;
import com.yoen.yoen_back.dto.etc.CategoryResponseDto;
import com.yoen.yoen_back.dto.etc.DestinationRequestDto;
import com.yoen.yoen_back.dto.etc.DestinationResponseDto;
import com.yoen.yoen_back.entity.travel.Destination;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.PaymentType;
import com.yoen.yoen_back.service.CommonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommonControllerTest {

    @Mock
    private CommonService commonService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CommonController(commonService)).build();
        objectMapper = new ObjectMapper();
    }

    // 카테고리 생성 요청을 서비스에 위임하고 ApiResponse로 감싸는지 확인한다.
    @Test
    void createCategory_validRequest_returnsCreatedCategories() throws Exception {
        List<CategoryRequestDto> request = List.of(new CategoryRequestDto(null, "식비", PaymentType.PAYMENT));
        when(commonService.createCategory(request))
                .thenReturn(List.of(new CategoryResponseDto(1L, "식비", PaymentType.PAYMENT)));

        mockMvc.perform(post("/common/category/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].categoryId").value(1))
                .andExpect(jsonPath("$.data[0].categoryName").value("식비"))
                .andExpect(jsonPath("$.data[0].type").value("PAYMENT"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(commonService).createCategory(request);
    }

    // 결제 타입 파라미터가 서비스 조회 조건으로 전달되는지 확인한다.
    @Test
    void getAllCategory_validType_returnsCategoriesByType() throws Exception {
        when(commonService.getCategoryListByType(PaymentType.PAYMENT))
                .thenReturn(List.of(new CategoryResponseDto(1L, "식비", PaymentType.PAYMENT)));

        mockMvc.perform(get("/common/category")
                        .param("type", "PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].categoryName").value("식비"));

        verify(commonService).getCategoryListByType(PaymentType.PAYMENT);
    }

    // nation 파라미터가 없으면 전체 목적지 조회로 분기하는지 확인한다.
    @Test
    void getNationDestinations_withoutNation_returnsAllDestinations() throws Exception {
        when(commonService.getAllDestination())
                .thenReturn(List.of(new DestinationResponseDto(1L, Nation.JAPAN, "도쿄")));

        mockMvc.perform(get("/common/destination/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].destinationId").value(1))
                .andExpect(jsonPath("$.data[0].destinationName").value("도쿄"));

        verify(commonService).getAllDestination();
    }

    // nation 파라미터가 있으면 국가별 목적지 조회로 분기하는지 확인한다.
    @Test
    void getNationDestinations_withNation_returnsNationDestinations() throws Exception {
        when(commonService.getNationDestinations(Nation.JAPAN))
                .thenReturn(List.of(new DestinationResponseDto(1L, Nation.JAPAN, "도쿄")));

        mockMvc.perform(get("/common/destination/all")
                        .param("nation", "JAPAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nation").value("JAPAN"));

        verify(commonService).getNationDestinations(Nation.JAPAN);
    }

    // 목적지 생성 엔드포인트는 ApiResponse 없이 서비스 결과 목록을 직접 반환하는지 확인한다.
    @Test
    void createDestination_validRequest_returnsDestinationsDirectly() throws Exception {
        List<DestinationRequestDto> request = List.of(new DestinationRequestDto("도쿄", Nation.JAPAN));
        Destination destination = Destination.builder()
                .destinationId(1L)
                .name("도쿄")
                .nation(Nation.JAPAN)
                .build();
        when(commonService.createDestinations(request)).thenReturn(List.of(destination));

        mockMvc.perform(post("/common/destination/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].destinationId").value(1))
                .andExpect(jsonPath("$[0].name").value("도쿄"))
                .andExpect(jsonPath("$[0].nation").value("JAPAN"));

        verify(commonService).createDestinations(request);
    }
}
