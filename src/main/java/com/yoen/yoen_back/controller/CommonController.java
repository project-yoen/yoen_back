package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.dto.etc.CategoryRequestDto;
import com.yoen.yoen_back.dto.etc.DestinationRequestDto;
import com.yoen.yoen_back.dto.etc.DestinationResponseDto;
import com.yoen.yoen_back.entity.travel.Destination;
import com.yoen.yoen_back.service.CommonService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT_AUTH")
@RequestMapping("/common")
public class CommonController {
    private final CommonService commonService;

    /** 카테고리 관련 엔드포인트 **/
    @PostMapping("/category/create")
    public ResponseEntity<ApiResponse<CategoryRequestDto>> createCategory(@RequestBody CategoryRequestDto dto) {
        CategoryRequestDto category = commonService.createCategory(dto);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    /** 목적지 관련 엔드포인트 **/
    @GetMapping("/destination/all")
    public ResponseEntity<ApiResponse<List<DestinationResponseDto>>> getAllTravelDestinations() {
        return ResponseEntity.ok(ApiResponse.success(commonService.getAllDestination()));
    }

    @PostMapping("/destination/create")
    public List<Destination> createDestination(@RequestBody List<DestinationRequestDto> destinations) {
        return commonService.createDestinations(destinations);
    }
}
