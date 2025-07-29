package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.travel.TravelRequestDto;
import com.yoen.yoen_back.dto.travel.TravelUserDto;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.TravelService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT_AUTH")
@RequestMapping("/travel")
public class TravelController {
    private final TravelService travelService;
    private final AuthService authService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Travel>>> travel() {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllTravels()));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Travel>> setTravel(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody TravelRequestDto dto) {
        Travel tv = travelService.createTravel(userDetails.user(), dto);
        return ResponseEntity.ok(ApiResponse.success(tv));
    }

    // TODO: 삭제 (미완) (읽기 권한)
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteTravel(@AuthenticationPrincipal CustomUserDetails userDetails,@RequestBody Long travelId) {
        Travel tv = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.WRITER));
        travelService.deleteTravel(tv);
        return ResponseEntity.ok(ApiResponse.success("삭제가 완료되었습니다."));
    }


    // 자신의 여행 유저 반환하는 함수 (읽기 권한)
    @GetMapping("/traveluser")
    public ResponseEntity<ApiResponse<TravelUserDto>> getTravelUser(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam Long travelId) {
        Travel tv = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        TravelUserDto tud = travelService.getTravelUser(userDetails.user(), tv);
        return ResponseEntity.ok(ApiResponse.success(tud));
    }


    // 여행에 포함된 여행유저들 반환하는 함수 (읽기 권한)
    @GetMapping("/traveluser/all")
    public List<TravelUserDto> getAllTravelUsers(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam Long travelId) {
        Travel tv = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        return travelService.getAllTravelUser(tv);
    }


}

