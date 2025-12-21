package com.roovies.concertreservation.points.application.port.in;

import com.roovies.concertreservation.points.application.dto.command.RewardPointCommand;
import com.roovies.concertreservation.points.application.dto.result.RewardPointResult;

/**
 * 포인트 적립 유스케이스.
 * <p>
 * 결제 완료 후 리워드 포인트를 적립하는 기능을 제공한다.
 */
public interface RewardPointUseCase {
    /**
     * 포인트를 적립한다.
     *
     * @param command 포인트 적립 요청 커맨드
     * @return 포인트 적립 결과
     */
    RewardPointResult reward(RewardPointCommand command);
}