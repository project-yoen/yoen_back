package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.travel.TravelRecordRequestDto;
import com.yoen.yoen_back.dto.travel.TravelRecordResponseDto;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/record")
public class RecordController {

    private final RecordService recordService;
    private final AuthService authService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<TravelRecord>>> travelRecord(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("travelId") Long travelId) {
        Travel tv = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        return ResponseEntity.ok(ApiResponse.success(recordService.getAllTravelRecordsByTravel(tv)));
    }


    // 여행 기록 작성하는 함수 (RequestParts를 쓰거나 아니면 두개로 분리해야함)
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<TravelRecordResponseDto>> setTravelRecord(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestPart("dto") TravelRecordRequestDto dto, @RequestPart(value = "images", required = false) List<MultipartFile> files) {
        authService.checkTravelUserRoleByTravel(userDetails.user(), dto.travelId(), List.of(Role.WRITER));
        TravelRecordResponseDto trd = recordService.createTravelRecord(userDetails.user(), dto, files);
        return ResponseEntity.ok(ApiResponse.success(trd));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TravelRecordResponseDto>>> getTravelRecordByDate(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("travelId") Long travelId, @RequestParam("date") String date) {
        Travel tv = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        return ResponseEntity.ok(ApiResponse.success(recordService.getTravelRecordsByDate(tv, date)));
    }


    // TODO: 업데이트 추가 해야됨

    // TODO: 삭제기능 추가 해야됨

}


