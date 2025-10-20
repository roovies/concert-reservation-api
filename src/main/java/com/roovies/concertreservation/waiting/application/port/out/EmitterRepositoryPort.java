package com.roovies.concertreservation.waiting.application.port.out;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface EmitterRepositoryPort {

    void saveEmitterByUserKey(String userKey, SseEmitter emitter);
    void removeEmitterByUserKey(String userKey);
    boolean containsEmitterByUserKey(String userKey);
    SseEmitter getEmitterByUserKey(String userKey);
}
