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

    @PostMapping("/setTravel")
    public ResponseEntity<ApiResponse<Travel>> setTravel(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody TravelRequestDto dto) {
        Travel tv = travelService.setTravel(userDetails.user(), dto);
        return ResponseEntity.ok(ApiResponse.success(tv));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> deleteTravel(@RequestBody Long travelId) {
        travelService.deleteTravel(travelId);
        return ResponseEntity.ok(ApiResponse.success("삭제가 완료되었습니다."));
    }

    @GetMapping("/record")
    public ResponseEntity<ApiResponse<List<TravelRecord>>> travelRecord(@RequestParam("travelId") Long travelId) {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllTravelRecordsByTravelId(travelId)));
    }

    @GetMapping("/payment")
    public ResponseEntity<ApiResponse<List<Payment>>> payment(@RequestParam("travelId") Long travelId) {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllPaymentsByTravelId(travelId)));
    }

    @PostMapping("/setPayment")
    public ResponseEntity<ApiResponse<Payment>> setPayment(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody PaymentRequestDto dto) {
        Payment pay = travelService.setPayment(dto);
        return ResponseEntity.ok(ApiResponse.success(pay));
    }

    @PostMapping("/setTravelRecord")
    public ResponseEntity<ApiResponse<TravelRecord>> setTravelRecord(@AuthenticationPrincipal CustomUserDetails userDetails,@RequestBody TravelRecordRequestDto dto) {
        TravelRecord tr = travelService.setTravelRecord(dto);
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

