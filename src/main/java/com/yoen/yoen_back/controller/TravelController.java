package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.*;
import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import com.yoen.yoen_back.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/travel")
public class TravelController {
    private final TravelService travelService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Travel>>> travel() {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllTravels()));
    }

    @GetMapping("/record")
    public ResponseEntity<ApiResponse<List<TravelRecord>>> travelRecord(@RequestParam("travelId") Long travelId) {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllTravelRecordsByTravelId(travelId)));
    }

    @GetMapping("/payment")
    public ResponseEntity<ApiResponse<List<Payment>>> payment(@RequestParam("travelId") Long travelId) {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllPaymentsByTravelId(travelId)));
    }


    // 미완, 일단 userId는 추후 뺼건데, travelUserNickname은 또 입력받아여해서 아마 dto를 써야할것 같음
    // 그리고 일단 보여주기 식 TravelUser 반환을 하는데 이것도 나중에 void나 일반 문자열출력으로 수정
    // dto, jwt 인증 구현 전까지는 PathVariable userId로 개발
    @PostMapping("/setTravel")
    public ResponseEntity<ApiResponse<Travel>> setTravel(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody TravelRequestDto travelRequestDto) {
        Travel tv = travelService.setTravel(userDetails.user(), travelRequestDto);
        return ResponseEntity.ok(ApiResponse.success(tv));
    }

    @PostMapping("/setPayment/{userId}")
    public ResponseEntity<ApiResponse<Payment>> setPayment(@PathVariable Long userId, @RequestBody PaymentRequestDto dto) {
        Payment pay = travelService.setPayment(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(pay));
    }

    @PostMapping("/setTravelRecord/{userId}")
    public ResponseEntity<ApiResponse<TravelRecord>> setTravelRecord(@PathVariable Long userId, @RequestBody TravelRecordRequestDto dto) {
        TravelRecord tr = travelService.setTravelRecord(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(tr));
    }

    @GetMapping("/code")
    public ResponseEntity<ApiResponse<JoinCodeResponseDto>> getCode(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("travelId") Long travelId) {
        String cd = travelService.getJoinCode(userDetails.user(), travelId);
        LocalDateTime expireTime = travelService.getCodeExpiredTime(cd);
        return ResponseEntity.ok(ApiResponse.success(JoinCodeResponseDto.joinCode(cd, expireTime)));
    }
    @GetMapping("/users")
    public List<TravelUserDto> getAllTravelUsers() {
        return travelService.getAllTravelUser().stream().map(tu -> new TravelUserDto(tu.getTravel().getTravelId(), tu.getUser().getUserId())).toList();
    }
    @GetMapping("/destinations")
    public List<TravelDestinationDto> getAllTravelDestinations() {
        return travelService.getAllTravelDestination().stream().map(td -> new TravelDestinationDto(td.getTravel().getTravelId(), td.getDestination().getName())).toList();
    }
}

