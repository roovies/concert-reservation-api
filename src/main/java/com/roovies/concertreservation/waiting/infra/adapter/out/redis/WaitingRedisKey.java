package com.roovies.concertreservation.waiting.infra.adapter.out.redis;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum WaitingRedisKey {
    SEMAPHORE("semaphore:%s:"),                 // ex. semaphore:reservation:
    WAITING("waiting:%s:"),                     // ex. waiting:reservation:
    ACTIVE_WAITING_ITEMS("active:waiting:%s"),  // ex. active:waiting:{schedules}
    ADMITTED_TOKEN("admitted:%s:"),             // ex. admitted:reservation:
    ADMIT_LOCK("lock:admit:%s");

    private final String pattern;

    WaitingRedisKey(final String pattern) {
        this.pattern = pattern;
    }

    public String generateKey(String domain, Object... params) {
        String keyWithDomain = String.format(pattern, domain);
        if (params.length > 0) {
            return keyWithDomain + Arrays.stream(params)
                    .map(Object::toString)
                    .collect(Collectors.joining(":"));
        }
        return keyWithDomain;
    }
}
