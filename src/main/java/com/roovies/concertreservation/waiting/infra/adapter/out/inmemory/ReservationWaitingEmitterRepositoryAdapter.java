package com.roovies.concertreservation.waiting.infra.adapter.out.inmemory;

import com.roovies.concertreservation.waiting.application.port.out.EmitterRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository("reservationWaitingEmitterRepository")
@RequiredArgsConstructor
public class ReservationWaitingEmitterRepositoryAdapter implements EmitterRepositoryPort {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    @Override
    public void saveEmitterByUserKey(String userKey, SseEmitter emitter) {
        emitterMap.put(userKey, emitter);
    }

    @Override
    public void removeEmitterByUserKey(String userKey) {
        emitterMap.remove(userKey);
    }

    @Override
    public boolean containsEmitterByUserKey(String userKey) {
        return emitterMap.containsKey(userKey);
    }

    @Override
    public SseEmitter getEmitterByUserKey(String userKey) {
        return emitterMap.get(userKey);
    }
}
