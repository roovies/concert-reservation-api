package com.roovies.concertreservation.payments.infra.adapter.in.web.controller;

import com.roovies.concertreservation.payments.infra.adapter.in.web.dto.enums.PaymentStatus;
import com.roovies.concertreservation.payments.infra.adapter.in.web.dto.request.CancelPaymentRequest;
import com.roovies.concertreservation.payments.infra.adapter.in.web.dto.request.CreatePaymentRequest;
import com.roovies.concertreservation.payments.infra.adapter.in.web.dto.response.CancelPaymentResponse;
import com.roovies.concertreservation.payments.infra.adapter.in.web.dto.response.CreatePaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment API", description = "콘서트 결제 관련 기능 명세서")
@SecurityRequirement(name = "bearerAuth")  // Swagger UI에 자물쇠 표시
public class PaymentController {

    @Operation(
            summary = "콘서트 예약 결제",
            description = """
                        좌석 예약 신청 완료 후, 보유 포인트를 사용해 결제한다.
                        잔액 차감과 예약 상태 변경은 하나의 트랜잭션으로 처리된다.
                        """
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "결제 성공",
                            content = @Content(schema = @Schema(implementation = CreatePaymentResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "리소스를 찾을 수 없음"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류"
                    )
            }
    )
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true, example = "Bearer {token}")
    })
    @PostMapping
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @AuthenticationPrincipal UserDetails userDetails, // TODO: Custom User Details 구현 필요
            @RequestBody CreatePaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                CreatePaymentResponse.builder()
                        .paymentId(1L)
                        .amount(BigDecimal.valueOf(120000))
                        .status(PaymentStatus.CONFIRMED)
                        .build()
        );
    }

    @Operation(
            summary = "결제 환불",
            description = """
                        사용자가 이미 완료한 결제를 취소(환불)한다.
                        환불 성공 시, 사용자 포인트를 원상 복구하고 해당 예약 상태를 취소(CANCELED) 로 변경한다.
                        """
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "환불 성공",
                            content = @Content(schema = @Schema(implementation = CancelPaymentResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "리소스를 찾을 수 없음"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류"
                    )
            }
    )
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true, example = "Bearer {token}")
    })
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<CancelPaymentResponse> cancelPayment(
            @AuthenticationPrincipal UserDetails userDetails, // TODO: Custom User Details 구현 필요
            @RequestBody CancelPaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                CancelPaymentResponse.builder()
                        .refundId(1L)
                        .amount(BigDecimal.valueOf(120000))
                        .build()
        );
    }
}
