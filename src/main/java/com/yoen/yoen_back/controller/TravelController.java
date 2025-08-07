package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.travel.*;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<TravelResponseDto>>> getAllTravelByUser(@AuthenticationPrincipal CustomUserDetails user) {
        List<TravelResponseDto> dtos = travelService.getAllTravelByUser(user.user());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Travel>>> travel() {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllTravels()));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<TravelResponseDto>> setTravel(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody TravelRequestDto dto) {
        TravelResponseDto responseDto = travelService.createTravel(userDetails.user(), dto);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    // TODO: 삭제 (미완) (읽기 권한)
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteTravel(@AuthenticationPrincipal CustomUserDetails userDetails,@RequestBody Long travelId) {
        TravelUser tu = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.WRITER));
        travelService.deleteTravel(tu.getTravel());
        return ResponseEntity.ok(ApiResponse.success("삭제가 완료되었습니다."));
    }


    // 자신의 여행 유저 반환하는 함수 (읽기 권한)
    @GetMapping("/traveluser")
    public ResponseEntity<ApiResponse<TravelUserDto>> getTravelUser(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam Long travelId) {
        TravelUser tu = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        TravelUserDto tud = travelService.getTravelUser(userDetails.user(), tu.getTravel());
        return ResponseEntity.ok(ApiResponse.success(tud));
    }

    @PostMapping("/traveluser/nickname")
    public ResponseEntity<ApiResponse<String>> updateTravelNickname(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody TravelNicknameUpdateDto dto) {
        TravelUser tu = authService.checkTravelUserRoleByTravel(userDetails.user(), dto.travelId(), List.of(Role.READER, Role.WRITER));
        travelService.updateTravelUserNickname(dto);
        return ResponseEntity.ok(ApiResponse.success("정상적으로 트레블 닉네임이 변경되었습니다."));
    }


    // 여행에 포함된 여행유저들 반환하는 함수 (읽기 권한)
    @GetMapping("/traveluser/all")
    public List<TravelUserDto> getAllTravelUsers(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam Long travelId) {
        TravelUser tu = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        return travelService.getAllTravelUser(tu.getTravel());
    }

    @GetMapping("/userdetail")
    public ResponseEntity<ApiResponse<List<TravelUserResponseDto>>> getDetailTravelUsers(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam Long travelId) {
        TravelUser tu = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        List<TravelUserResponseDto> dtos = travelService.getDetailTravelUser(tu.getTravel());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping("/leave/{travelId}")
    public ResponseEntity<ApiResponse<String>> leaveTravel(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("travelId") Long travelId) {
        TravelUser tu = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        travelService.leaveTravel(tu);
        return ResponseEntity.ok(ApiResponse.success("정상적으로 여행에서 나가졌습니다."));
    }

    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<TravelResponseDto>> getTravelDetail(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam Long travelId) {
        authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        TravelResponseDto dto = travelService.getTravelDetail(travelId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}

