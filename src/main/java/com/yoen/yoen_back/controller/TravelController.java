package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelRecord;
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


    // 미완
    @PostMapping("/setTravel")
    public ResponseEntity<Object> setTravel(@RequestBody Travel travel) {
        try {
//            travelService.setTravel(travel);
            return ResponseEntity.ok("Travel Saved Successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
