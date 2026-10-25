package kuchma.mike.bbdm.presentation;

import kuchma.mike.bbdm.application.ScriptingAdventureService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(path = "/api/scripting-adventure/sessions", version = GameController.API_VERSION)
public class ScriptingAdventureController {
    private final ScriptingAdventureService scriptingAdventureService;

    private final ScriptingAdventureEventStream eventStream;

    public ScriptingAdventureController(
        final ScriptingAdventureService scriptingAdventureService,
        final ScriptingAdventureEventStream eventStream
    ) {
        this.scriptingAdventureService = scriptingAdventureService;
        this.eventStream = eventStream;
    }

    @GetMapping("/{sessionId}/events")
    public SseEmitter subscribe(@PathVariable("sessionId") final String sessionId) {
        return eventStream.subscribe(sessionId);
    }

    @PostMapping("/{sessionId}")
    public ResponseEntity<SessionAccepted> startSession(@PathVariable("sessionId") final String sessionId) {
        scriptingAdventureService.startSession(sessionId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new SessionAccepted(sessionId));
    }

    public record SessionAccepted(String sessionId) {
    }
}
