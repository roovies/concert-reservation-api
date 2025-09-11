package com.roovies.concertreservation.concerthalls.domain.entity;

import com.roovies.concertreservation.concerthalls.domain.enums.SeatType;
import com.roovies.concertreservation.concerthalls.domain.vo.Money;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ConcertHallSeat {
    private final Long id;
    private final int row;
    private final int seatNumber;
    private final SeatType seatType;
    private final Money price;
    private final LocalDateTime createdAt;

    public static ConcertHallSeat create(Long id, int row, int seatNumber, SeatType seatType, Money price, LocalDateTime createdAt) {
        return new ConcertHallSeat(id, row, seatNumber, seatType, price, createdAt);
    }

    private ConcertHallSeat(Long id, int row, int seatNumber, SeatType seatType, Money price, LocalDateTime createdAt) {
        this.id = id;
        this.row = row;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.price = price;
        this.createdAt = createdAt;
    }
}
