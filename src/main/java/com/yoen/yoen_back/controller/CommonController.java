package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.dto.etc.CategoryRequestDto;
import com.yoen.yoen_back.dto.etc.DestinationDto;
import com.yoen.yoen_back.dto.travel.TravelDestinationDto;
import com.yoen.yoen_back.entity.travel.Destination;
import com.yoen.yoen_back.service.CommonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
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
    public List<TravelDestinationDto> getAllTravelDestinations() {
        return commonService.getAllTravelDestination().stream().map(
                td -> new TravelDestinationDto(td.getTravel().getTravelId(), td.getDestination().getName())
        ).toList();
    }

    @PostMapping("/destination/create")
    public List<Destination> createDestination(@RequestBody List<DestinationDto> destinations) {
        return commonService.createDestinations(destinations);
    }
}
