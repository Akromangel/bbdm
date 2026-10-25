package kuchma.mike.bbdm.presentation;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import kuchma.mike.bbdm.application.ScriptingAdventureStateEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

@Service
public class ScriptingAdventureEventStream {
    private static final Logger log = LoggerFactory.getLogger(ScriptingAdventureEventStream.class);

    private final ConcurrentMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(final String sessionId) {
        SseEmitter emitter = new SseEmitter(0L);
        SseEmitter previous = emitters.put(sessionId, emitter);
        if (previous != null) {
            previous.complete();
        }

        Runnable removeEmitter = () -> emitters.remove(sessionId, emitter);
        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(removeEmitter);
        emitter.onError(_ -> removeEmitter.run());
        try {
            SseEventBuilder connectedEvent = SseEmitter.event()
                .name("connected")
                .data(new StreamConnected(sessionId));
            emitter.send(connectedEvent);
        } catch (IOException exception) {
            emitters.remove(sessionId, emitter);
            log.atWarn()
                .addKeyValue("sessionId", sessionId)
                .addKeyValue("error", exception.getMessage())
                .setMessage("Could not initialize scripting adventure SSE stream")
                .log();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @EventListener
    public void publish(final ScriptingAdventureStateEvent event) {
        SseEmitter emitter = emitters.get(event.sessionId());
        if (emitter == null) {
            log.atWarn()
                .addKeyValue("sessionId", event.sessionId())
                .addKeyValue("turn", event.turn())
                .setMessage("Dropping scripting adventure state because no SSE client is connected")
                .log();
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("state").data(event));
            if (event.complete()) {
                emitter.complete();
                emitters.remove(event.sessionId(), emitter);
            }
        } catch (IOException exception) {
            log.atWarn()
                .addKeyValue("sessionId", event.sessionId())
                .addKeyValue("error", exception.getMessage())
                .setMessage("Could not send scripting adventure state over SSE")
                .log();
            emitters.remove(event.sessionId(), emitter);
            emitter.completeWithError(exception);
        }
    }

    public record StreamConnected(String sessionId) {
    }
}
