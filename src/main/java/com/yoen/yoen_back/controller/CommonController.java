package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.dto.etc.CategoryRequestDto;
import com.yoen.yoen_back.dto.etc.CategoryResponseDto;
import com.yoen.yoen_back.dto.etc.DestinationRequestDto;
import com.yoen.yoen_back.dto.etc.DestinationResponseDto;
import com.yoen.yoen_back.entity.travel.Destination;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.PaymentType;
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
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> createCategory(@RequestBody List<CategoryRequestDto> dto) {
        List<CategoryResponseDto> category = commonService.createCategory(dto);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @GetMapping("/category")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getAllCategory(@RequestParam(value = "type") PaymentType type) {
        List<CategoryResponseDto> dtos = commonService.getCategoryListByType(type);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /** 목적지 관련 엔드포인트 **/
    @GetMapping("/destination/all")
    public ResponseEntity<ApiResponse<List<DestinationResponseDto>>> getNationDestinations(@RequestParam(value = "nation", required = false) Nation nation) {
        if (nation == null) return ResponseEntity.ok(ApiResponse.success(commonService.getAllDestination()));
        return ResponseEntity.ok(ApiResponse.success(commonService.getNationDestinations(nation)));
    }

    @PostMapping("/destination/create")
    public List<Destination> createDestination(@RequestBody List<DestinationRequestDto> destinations) {
        return commonService.createDestinations(destinations);
    }
}
