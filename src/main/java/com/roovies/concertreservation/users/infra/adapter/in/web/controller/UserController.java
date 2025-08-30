package com.roovies.concertreservation.users.infra.adapter.in.web.controller;

import com.roovies.concertreservation.users.infra.adapter.in.web.dto.request.CreateUserRequest;
import com.roovies.concertreservation.users.infra.adapter.in.web.dto.request.DeleteUserRequest;
import com.roovies.concertreservation.users.infra.adapter.in.web.dto.request.UpdatePasswordRequest;
import com.roovies.concertreservation.users.infra.adapter.in.web.dto.request.UpdateUserRequest;
import com.roovies.concertreservation.users.infra.adapter.in.web.dto.response.GetMyInformationResponse;
import com.roovies.concertreservation.users.infra.adapter.in.web.dto.response.UpdateUserResponse;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "User API", description = "사용자 관련 기능 명세서")
public class UserController {

    @Operation(
            summary = "회원가입",
            description = "사용자는 서비스 이용을 위해 이메일/비밀번호 기반 회원가입을 수행한다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "회원가입 성공",
                            content = @Content(schema = @Schema(implementation = Long.class)) // 응답이 Long 타입임을 명시
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류"
                    )
            }
    )
    @PostMapping
    public ResponseEntity<Long> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(0L);
    }

    @Operation(
            summary = "내정보 조회",
            description = "인증된 사용자는 자신의 이메일, 닉네임, 이름, 가입일을 조회할 수 있다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "내정보 조회 성공",
                            content = @Content(schema = @Schema(implementation = GetMyInformationResponse.class))
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
    @GetMapping("/me")
    public ResponseEntity<GetMyInformationResponse> getMyInformation(@AuthenticationPrincipal UserDetails userDetails) { // TODO: Custom User Details 구현 필요
        return ResponseEntity.status(HttpStatus.OK).body(GetMyInformationResponse.builder()
                        .email("test@naver.com")
                        .name("이지환")
                        .nickname("루비즈")
                        .createdAt(LocalDate.now())
                        .build());
    }

    @Operation(
            summary = "내정보 수정",
            description = "인증된 사용자는 자신의 이메일/이름/닉네임을 수정할 수 있다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "내정보 수정 성공",
                            content = @Content(schema = @Schema(implementation = UpdateUserResponse.class))
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
    @PatchMapping("/me")
    public ResponseEntity<UpdateUserResponse> updateMyInformation(
            @AuthenticationPrincipal UserDetails userDetails, // TODO: Custom User Details 구현 필요
            @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(UpdateUserResponse.builder()
                        .email("test@naver.com")
                        .name("이지환")
                        .nickname("뉴루비즈")
                        .build());
    }

    @Operation(
            summary = "패스워드 수정",
            description = "인증된 사용자는 자신의 비밀번호를 수정할 수 있다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "패스워드 수정 성공"
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
    @PutMapping("/me/password") // password라는 리소스 전체를 업데이트 하기 때문에 PUT
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails, // TODO: Custom User Details 구현 필요
            @RequestBody UpdatePasswordRequest request
    ) {
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "회원탈퇴",
            description = "인증된 사용자는 현재 비밀번호를 입력하여 회원탈퇴를 할 수 있다. (soft delete)"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "회원탈퇴 성공"
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
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails, // TODO: Custom User Details 구현 필요
            @RequestBody DeleteUserRequest request
    ) {
        return ResponseEntity.ok().build();
    }
}
