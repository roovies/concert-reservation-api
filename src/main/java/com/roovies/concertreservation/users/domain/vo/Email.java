package com.roovies.concertreservation.users.domain.vo;

public record Email(
        String value
) {
    public static Email of(String value) {
        return new Email(value);
    }
}
