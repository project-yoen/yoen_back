package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.dto.ApiResponse;
import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/travel")
public class TravelController {
    private final TravelService travelService;

    @GetMapping
    public ResponseEntity<List<Travel>> travel() {
        return ResponseEntity.ok(travelService.getAllTravels());
    }

    @GetMapping("/record")
    public ResponseEntity<List<TravelRecord>> travelRecord(@RequestParam("travelId") Long travelId) {
        return ResponseEntity.ok(travelService.getAllTravelRecordsByTravelId(travelId));
    }

    @GetMapping("/payment")
    public ResponseEntity<List<Payment>> payment(@RequestParam("travelId") Long travelId) {
        return ResponseEntity.ok(travelService.getAllPaymentsByTravelId(travelId));
    }


    // 미완, 일단 userId는 추후 뺼건데, travelUserNickname은 또 입력받아여해서 아마 dto를 써야할것 같음
    // 그리고 일단 보여주기 식 TravelUser 반환을 하는데 이것도 나중에 void나 일반 문자열출력으로 수정
    @PostMapping("/setTravel/{userId}")
    public ResponseEntity<ApiResponse<TravelUser>> setTravel(@PathVariable Long userId, @RequestBody Travel travel) {
        try {
            TravelUser tu = travelService.setTravel(userId, travel);
            return ResponseEntity.ok(ApiResponse.success(tu));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(e.getMessage()));
        }
    }

}
