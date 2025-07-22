package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.*;
import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.payment.SettlementUser;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/travel")
public class TravelController {
    private final TravelService travelService;

    @GetMapping("/get-alltravel")
    public ResponseEntity<ApiResponse<List<Travel>>> travel() {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllTravels()));
    }

    @PostMapping("/set-travel")
    public ResponseEntity<ApiResponse<Travel>> setTravel(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody TravelRequestDto dto) {
        Travel tv = travelService.setTravel(userDetails.user(), dto);
        return ResponseEntity.ok(ApiResponse.success(tv));
    }

    @DeleteMapping("/delete-travel")
    public ResponseEntity<ApiResponse<String>> deleteTravel(@RequestBody Long travelId) {
        travelService.deleteTravel(travelId);
        return ResponseEntity.ok(ApiResponse.success("삭제가 완료되었습니다."));
    }

    @GetMapping("/get-record")
    public ResponseEntity<ApiResponse<List<TravelRecord>>> travelRecord(@RequestParam("travelId") Long travelId) {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllTravelRecordsByTravelId(travelId)));
    }

    @GetMapping("/get-payment")
    public ResponseEntity<ApiResponse<List<Payment>>> payment(@RequestParam("travelId") Long travelId) {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllPaymentsByTravelId(travelId)));
    }

    // 여행 기록 작성하는 함수 (RequestParts를 쓰거나 아니면 두개로 분리해야함)
    @PostMapping("/set-travelrecord")
    public ResponseEntity<ApiResponse<TravelRecordResponseDto>> setTravelRecord(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestPart("dto") TravelRecordRequestDto dto, @RequestPart("images") List<MultipartFile> files) {
        TravelRecordResponseDto trd = travelService.createTravelRecord(userDetails.user(), dto, files);
        return ResponseEntity.ok(ApiResponse.success(trd));
    }

    // 여행 유저 반환하는 함수
    @GetMapping("/get-traveluser")
    public ResponseEntity<ApiResponse<TravelUserDto>> getTravelUser(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam Long travelId) {
        TravelUser tu = travelService.getTravelUser(userDetails.user(), travelId);
        TravelUserDto tud = new TravelUserDto(tu.getTravelUserId(), tu.getUser().getUserId(), tu.getTravel().getTravelId());
        return ResponseEntity.ok(ApiResponse.success(tud));
    }

    @GetMapping("/get-code")
    public ResponseEntity<ApiResponse<JoinCodeResponseDto>> getCode(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("travelId") Long travelId) {
        String cd = travelService.getJoinCode(userDetails.user(), travelId);
        LocalDateTime expireTime = travelService.getCodeExpiredTime(cd);
        return ResponseEntity.ok(ApiResponse.success(JoinCodeResponseDto.joinCode(cd, expireTime)));
    }

    @GetMapping("/get-alluser")
    public List<TravelUserDto> getAllTravelUsers() {
        return travelService.getAllTravelUser().stream().map(tu -> new TravelUserDto(tu.getTravelUserId(), tu.getTravel().getTravelId(), tu.getUser().getUserId())).toList();
    }

    @GetMapping("/get-alldestination")
    public List<TravelDestinationDto> getAllTravelDestinations() {
        return travelService.getAllTravelDestination().stream().map(td -> new TravelDestinationDto(td.getTravel().getTravelId(), td.getDestination().getName())).toList();
    }

    @PostMapping("/set-payment")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> createTravelPayment(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestPart("dto") PaymentRequestDto dto, @RequestPart("images") List<MultipartFile> files) {
        PaymentResponseDto responseDto = travelService.createTravelPayment(userDetails.user(), dto, files);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    @PostMapping("/set-category")
    public ResponseEntity<ApiResponse<CategoryRequestDto>> craeteCategory(@RequestBody CategoryRequestDto dto) {
        CategoryRequestDto category = travelService.createCategory(dto);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    // 테스트용
    @GetMapping("/get-allsettlementuser")
    public ResponseEntity<ApiResponse<List<SettlementUserResponseDto>>> getAllSettlementUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<SettlementUser> settlementUserList = travelService.getAllSettlementUsers();
        List<SettlementUserResponseDto> surd = settlementUserList.stream().map(settlementUser -> new SettlementUserResponseDto(settlementUser.getSettlementUserId(), settlementUser.getSettlement().getSettlementId(), settlementUser.getTravelUser().getTravelUserId(), settlementUser.getAmount(), settlementUser.getIsPaid())).toList();

        return ResponseEntity.ok(ApiResponse.success(surd));
    }
}

