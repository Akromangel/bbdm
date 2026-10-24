package kuchma.mike.bbdm.presentation;

import kuchma.mike.bbdm.domain.model.StartedGame;
import kuchma.mike.bbdm.domain.port.GameGateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tabletest.junit.TableTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerVersionTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameGateway gameGateway;

    @TableTest("""
        version
        "1.0"
        """)
    void shouldDeclareTheApiVersionOnTheControllerMapping(String version) {
        RequestMapping mapping = GameController.class.getAnnotation(RequestMapping.class);

        assertThat(mapping.version()).isEqualTo(version);
    }

    @TableTest("""
        path
        "/api"
        """)
    void shouldDeclareTheApiPathOnTheControllerMapping(String path) {
        RequestMapping mapping = GameController.class.getAnnotation(RequestMapping.class);

        assertThat(mapping.path()[0]).isEqualTo(path);
    }

    @Test
    void shouldResolveTheVersionFromTheApiVersionRequestHeader() throws Exception {
        when(gameGateway.startGame()).thenReturn(new StartedGame("game-1", 3, 10, 1, 0, 0, 0));

        MvcResult result = mockMvc.perform(post("/api/game").header("API-Version", "1.0"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void shouldRequireTheApiVersionHeaderForApiRequests() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldReturnTheStartedGamePayloadForTheApiVersionRequestHeader() throws Exception {
        when(gameGateway.startGame()).thenReturn(new StartedGame("game-1", 3, 10, 1, 0, 0, 0));

        MvcResult result = mockMvc.perform(post("/api/game").header("API-Version", "1.0"))
            .andReturn();

        assertThat(result.getResponse().getContentAsString())
            .matches("(?s).*\"gameId\"\\s*:\\s*\"game-1\".*");
    }

    @TableTest("""
        path
        "/game"
        "/scripting-adventure"
        """)
    void shouldForwardFrontendRoutesToTheApplicationEntryPoint(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();

        assertThat(result.getResponse().getForwardedUrl()).isEqualTo("/index.html");
    }
}
