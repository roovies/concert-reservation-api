package com.roovies.concertreservation.users.infra.adapter.in.web;

import com.roovies.concertreservation.users.infra.adapter.in.web.request.LoginRequest;
import com.roovies.concertreservation.users.infra.adapter.in.web.request.LogoutRequest;
import com.roovies.concertreservation.users.infra.adapter.in.web.request.ReissueRequest;
import com.roovies.concertreservation.users.infra.adapter.in.web.response.LoginResponse;
import com.roovies.concertreservation.users.infra.adapter.in.web.response.ReissueResponse;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth API", description = "사용자 인증/인가 관련 명세서")
public class AuthController {

    @Operation(
            summary = "로그인",
            description = "가입된 사용자는 이메일, 비밀번호를 통해 로그인에 성공하면 JWT를 발급받는다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그인 성공",
                            content = @Content(schema = @Schema(implementation = LoginResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
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
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(
                LoginResponse.builder()
                        .accessToken("accessToken")
                        .refreshToken("refreshToken")
                        .accessTokenExpiresIn(60000L)
                        .refreshTokenExpiresIn(120000L)
                        .memberInfo(
                                LoginResponse.MemberInfo.builder()
                                        .id(1L)
                                        .email("test@naver.com")
                                        .nickname("루비즈")
                                        .build()
                        )
                        .build());
    }

    @Operation(
            summary = "액세스 토큰 및 리프레시 토큰 재발급(RTR)",
            description = "액세스 토큰이 만료되면 리프레시 토큰을 전송하여 액세스 토큰 및 리프레시 토큰을 재발급 받을 수 있다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "토큰 재발급 성공",
                            content = @Content(schema = @Schema(implementation = ReissueResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
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
    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ReissueResponse.builder()
                        .accessToken("accessToken")
                        .refreshToken("refreshToken")
                        .accessTokenExpiresIn(60000L)
                        .refreshTokenExpiresIn(120000L)
                        .build()
                );
    }

    @Operation(
            summary = "로그아웃",
            description = "로그아웃 시 캐싱되고 있는 리프레시 토큰을 삭제하여 로그아웃 처리를 한다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "로그아웃 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
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
    @SecurityRequirement(name = "bearerAuth")  // Swagger UI에 자물쇠 표시
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true, example = "Bearer {token}")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails userDetails, // TODO: Custom User Details 구현 필요
            @Valid @RequestBody LogoutRequest request
    ) {
        return ResponseEntity.ok().build();
    }



}
