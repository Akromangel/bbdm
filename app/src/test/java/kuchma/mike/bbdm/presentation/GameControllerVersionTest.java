package kuchma.mike.bbdm.presentation;

import kuchma.mike.bbdm.domain.port.GameGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerVersionTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameGateway gameGateway;

    @Test
    void declaresTheApiVersionOnTheControllerMapping() {
        RequestMapping mapping = GameController.class.getAnnotation(RequestMapping.class);

        assertEquals("1.0", mapping.version());
        assertEquals("/api", mapping.path()[0]);
    }

    @Test
    void resolvesTheVersionFromTheApiVersionRequestHeader() throws Exception {
        when(gameGateway.startGame()).thenReturn(Map.of("gameId", "game-1"));

        mockMvc.perform(post("/api/game").header("API-Version", "1.0"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"gameId\":\"game-1\"}"));
    }
}
