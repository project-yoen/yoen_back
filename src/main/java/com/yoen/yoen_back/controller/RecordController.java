package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.travel.TravelRecordRequestDto;
import com.yoen.yoen_back.dto.travel.TravelRecordResponseDto;
import com.yoen.yoen_back.entity.travel.TravelRecord;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<TravelRecordResponseDto>>> getTravelRecordByDate(@RequestParam("travelUserId") Long travelUserId, @RequestParam("date") String date) {
        return ResponseEntity.ok(ApiResponse.success(recordService.getTravelRecordsByDate(travelUserId, date)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<TravelRecord>>> travelRecord(@RequestParam("travelId") Long travelId) {
        return ResponseEntity.ok(ApiResponse.success(recordService.getAllTravelRecordsByTravelId(travelId)));
    }


    // 여행 기록 작성하는 함수 (RequestParts를 쓰거나 아니면 두개로 분리해야함)
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<TravelRecordResponseDto>> setTravelRecord(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestPart("dto") TravelRecordRequestDto dto, @RequestPart(value = "images", required = false) List<MultipartFile> files) {
        TravelRecordResponseDto trd = recordService.createTravelRecord(userDetails.user(), dto, files);
        return ResponseEntity.ok(ApiResponse.success(trd));
    }

    // TODO: 업데이트 추가 해야됨

    // TODO: 삭제기능 추가 해야됨

}


