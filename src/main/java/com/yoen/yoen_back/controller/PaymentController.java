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
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.enums.PaymentType;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumSet;
import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT_AUTH")
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentSimpleResponseDto>>> getSimplePayment(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("travelId") Long travelId, @RequestParam(value = "date", required = false) String date, @RequestParam(value = "type", required = false)PaymentType type) {
        TravelUser tu = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        List<PaymentSimpleResponseDto> dtos = paymentService.getAllPaymentResponseDtoByTravelIdAndDate(tu.getTravel(), date, type);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getDetailPayment(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("paymentId") Long paymentId) {
        authService.checkTravelUserRoleByPayment(userDetails.user(), paymentId, List.of(Role.READER, Role.WRITER));
        PaymentResponseDto dto = paymentService.getDetailPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Payment>>> payment(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("travelId") Long travelId) {
        TravelUser tu = authService.checkTravelUserRoleByTravel(userDetails.user(), travelId, List.of(Role.READER, Role.WRITER));
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAllPaymentsByTravel(tu.getTravel())));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> createTravelPayment(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestPart("dto") PaymentRequestDto dto, @RequestPart(value = "images", required = false) List<MultipartFile> files) {
        authService.checkTravelUserRoleByTravel(userDetails.user(), dto.travelId(), List.of(Role.WRITER));
        PaymentResponseDto responseDto = paymentService.createPayment(userDetails.user(), dto, files);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    // 테스트용 (앱에서 사용 X)
    @GetMapping("/settlementuser/all")
    public ResponseEntity<ApiResponse<List<SettlementUserResponseDto>>> getAllSettlementUser() {
        List<SettlementUserResponseDto> dtos = paymentService.getAllSettlementUsers();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    // TODO: 업데이트 추가 해야됨

    @DeleteMapping("/image/delete")
    public ResponseEntity<ApiResponse<String>> deletePaymentImage(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("imageId") Long paymentImageId) {
        authService.checkTravelUserRoleByPaymentImage(userDetails.user(), paymentImageId, List.of(Role.WRITER));
        paymentService.deletePaymentImage(paymentImageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully"));
    }


    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deletePayment(@AuthenticationPrincipal CustomUserDetails userDetails,@RequestParam("paymentId") Long paymentId) {
        authService.checkTravelUserRoleByPayment(userDetails.user(), paymentId, List.of(Role.WRITER));
        paymentService.deletePayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment deleted successfully"));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<String>> updatePayment(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestPart("dto") PaymentRequestDto dto, @RequestPart(value = "images", required = false) List<MultipartFile> files) {
        authService.checkTravelUserRoleByTravel(userDetails.user(), dto.travelId(), List.of(Role.WRITER));
        paymentService.updatePayment(userDetails.user(), dto, files);
        return ResponseEntity.ok(ApiResponse.success("Payment updated successfully"));
    }
    // 테스트용 (필요 시 PaymentService의 메서드 public으로 바꾸고 사용)
//  @DeleteMapping("/settlement/delete")
//  public ResponseEntity<ApiResponse<String>> deleteSettlement(@RequestParam("settlementId") Long settlementId) {
//      paymentService.deleteSettlement(settlementId);
//      return ResponseEntity.ok(ApiResponse.success("Settlement deleted successfully"));
//  }
}

