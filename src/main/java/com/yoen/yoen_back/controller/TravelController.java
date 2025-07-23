package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.travel.TravelRequestDto;
import com.yoen.yoen_back.dto.travel.TravelUserDto;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/travel")
public class TravelController {
    private final TravelService travelService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Travel>>> travel() {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllTravels()));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Travel>> setTravel(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody TravelRequestDto dto) {
        Travel tv = travelService.createTravel(userDetails.user(), dto);
        return ResponseEntity.ok(ApiResponse.success(tv));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteTravel(@RequestBody Long travelId) {
        travelService.deleteTravel(travelId);
        return ResponseEntity.ok(ApiResponse.success("삭제가 완료되었습니다."));
    }


    // 여행 유저 반환하는 함수
    @GetMapping("/traveluser")
    public ResponseEntity<ApiResponse<TravelUserDto>> getTravelUser(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam Long travelId) {
        TravelUser tu = travelService.getTravelUser(userDetails.user(), travelId);
        TravelUserDto tud = new TravelUserDto(tu.getTravelUserId(), tu.getUser().getUserId(), tu.getTravel().getTravelId());
        return ResponseEntity.ok(ApiResponse.success(tud));
    }


    @GetMapping("/traveluser/all")
    public List<TravelUserDto> getAllTravelUsers() {
        return travelService.getAllTravelUser().stream().map(tu -> new TravelUserDto(tu.getTravelUserId(), tu.getTravel().getTravelId(), tu.getUser().getUserId())).toList();
    }


}

