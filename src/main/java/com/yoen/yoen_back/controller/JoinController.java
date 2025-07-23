package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.etc.joincode.AcceptJoinRequestDto;
import com.yoen.yoen_back.dto.etc.joincode.JoinCodeResponseDto;
import com.yoen.yoen_back.dto.etc.joincode.JoinRequestListResponseDto;
import com.yoen.yoen_back.dto.etc.joincode.UserTravelJoinResponseDto;
import com.yoen.yoen_back.service.JoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/join")
public class JoinController {

    private final JoinService joinService;

    @GetMapping("/code")
    public ResponseEntity<ApiResponse<JoinCodeResponseDto>> getCode(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("travelId") Long travelId) {
        String cd = joinService.getJoinCode(userDetails.user(), travelId);
        LocalDateTime expireTime = joinService.getCodeExpiredTime(cd);
        return ResponseEntity.ok(ApiResponse.success(JoinCodeResponseDto.joinCode(cd, expireTime)));
    }

    @GetMapping("/userlist")
    public ResponseEntity<ApiResponse<List<UserTravelJoinResponseDto>>> getUserTravelJoinList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(joinService.getUserTravelJoinRequests(userDetails.user())));
    }

    @DeleteMapping("/delete/{id}")
    public void deleteUserTravelJoinRequest(@PathVariable("id") Long travelJoinRequestId) {
        joinService.deleteUserTravelJoinRequest(travelJoinRequestId);
    }

    // 여행 유저 반환하는 함수
    @PostMapping("/{joinCode}")
    public ResponseEntity<ApiResponse<String>> setTravelJoinRequest(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("joinCode") String joinCode) {
        joinService.requestToJoinTravel(userDetails.user(), joinCode);
        return ResponseEntity.ok(ApiResponse.success(""));
    }

    // 여행에 참여 신청한 사람들 출력
    @GetMapping("/travellist")
    public ResponseEntity<ApiResponse<List<JoinRequestListResponseDto>>> getTravelJoinRequest(@RequestParam("travelId") Long travelId) {
        List<JoinRequestListResponseDto> dtos = joinService.getJoinRequestList(travelId);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    // 여행에 참여 신청한 사람 승인
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<String>> acceptJoinRequest(@RequestBody AcceptJoinRequestDto dto) {
        joinService.acceptJoinRequest(dto);
        return ResponseEntity.ok(ApiResponse.success("Join request accepted"));
    }

    //여행에 참여 신청한 사람 거절
    @PostMapping("/reject/{id}")
    public ResponseEntity<ApiResponse<String>> rejectJoinRequest(@PathVariable("id") Long travelJoinRequestId) {
        joinService.rejectJoinRequest(travelJoinRequestId);
        return ResponseEntity.ok(ApiResponse.success("Join request rejected"));
    }
}
