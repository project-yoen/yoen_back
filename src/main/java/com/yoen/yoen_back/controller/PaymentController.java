package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.payment.PaymentRequestDto;
import com.yoen.yoen_back.dto.payment.PaymentResponseDto;
import com.yoen.yoen_back.dto.payment.PaymentSimpleResponseDto;
import com.yoen.yoen_back.dto.payment.settlement.SettlementUserResponseDto;
import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.payment.SettlementUser;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentSimpleResponseDto>>> getSimplePayment(@RequestParam("travelUserId") Long travelUserId, @RequestParam("date") String date) {
        List<PaymentSimpleResponseDto> dtos = paymentService.getAllPaymentResponseDtoByTravelId(travelUserId, date);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getDetailPayment(@RequestParam("paymentId")Long paymentId) {
        PaymentResponseDto dto = paymentService.getDetailPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Payment>>> payment(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("travelId") Long travelId) {
        Travel tv = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAllPaymentsByTravel(tv)));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> createTravelPayment(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestPart("dto") PaymentRequestDto dto, @RequestPart(value = "images", required = false) List<MultipartFile> files) {
        PaymentResponseDto responseDto = paymentService.createPayment(userDetails.user(), dto, files);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    // 테스트용
    @GetMapping("/settlementuser/all")
    public ResponseEntity<ApiResponse<List<SettlementUserResponseDto>>> getAllSettlementUser() {
        List<SettlementUser> settlementUserList = paymentService.getAllSettlementUsers();
        List<SettlementUserResponseDto> surd = settlementUserList.stream().map(settlementUser -> new SettlementUserResponseDto(settlementUser.getSettlementUserId(), settlementUser.getSettlement().getSettlementId(), settlementUser.getTravelUser().getTravelUserId(), settlementUser.getAmount(), settlementUser.getIsPaid())).toList();

        return ResponseEntity.ok(ApiResponse.success(surd));
    }

    // TODO: 업데이트 추가 해야됨

    @DeleteMapping("/image/delete")
    public ResponseEntity<ApiResponse<String>> deletePaymentImage(@RequestParam("imageId") Long paymentImageId) {
        paymentService.deletePaymentImage(paymentImageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully"));
    }

    @DeleteMapping("/settlement/delete")
    public ResponseEntity<ApiResponse<String>> deleteSettlement(@RequestParam("settlementId") Long settlementId) {
        paymentService.deleteSettlement(settlementId);
        return ResponseEntity.ok(ApiResponse.success("Settlement deleted successfully"));
    }

    @DeleteMapping("/delete-payment")
    public ResponseEntity<ApiResponse<String>> deletePayment(@RequestParam("paymentId") Long paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment deleted successfully"));
    }
}
