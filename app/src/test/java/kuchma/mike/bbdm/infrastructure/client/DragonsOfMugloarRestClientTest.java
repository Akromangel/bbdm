package kuchma.mike.bbdm.infrastructure.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DragonsOfMugloarRestClientTest {
    private static final String BASE_URL = "https://dragonsofmugloar.com";
    private static final String GAME_ID = "game-1";

    @Test
    void callsTheDocumentedUpstreamEndpoints() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        DragonsOfMugloarRestClient client = createClient(restClientBuilder);

        expectRequest(server, HttpMethod.POST, "/api/v2/game/start");
        expectRequest(server, HttpMethod.GET, "/api/v2/" + GAME_ID + "/messages");
        expectRequest(server, HttpMethod.POST, "/api/v2/" + GAME_ID + "/solve/ad-1");
        expectRequest(server, HttpMethod.GET, "/api/v2/" + GAME_ID + "/shop");
        expectRequest(server, HttpMethod.POST, "/api/v2/" + GAME_ID + "/shop/buy/item-1");

        client.startGame();
        client.getMessages(GAME_ID);
        client.dealWithMessage(GAME_ID, "ad-1", new DragonsOfMugloarRestClient.DealMessageRequest("true"));
        client.getShop(GAME_ID);
        client.buyItem(GAME_ID, "item-1");

        server.verify();
    }

    @Test
    void keepsResponseBodyReadableWhenDebugLoggingIsEnabled() {
        Logger logger = (Logger) LoggerFactory.getLogger(UpstreamLoggingInterceptor.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);

        try {
            RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
            MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
            server.expect(requestTo(BASE_URL + "/api/v2/game/start"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"gameId\":\"" + GAME_ID + "\"}", MediaType.APPLICATION_JSON));

            Object response = createClient(restClientBuilder).startGame();

            assertEquals(Map.of("gameId", GAME_ID), response);
            server.verify();
        } finally {
            logger.setLevel(previousLevel);
        }
    }

    private static DragonsOfMugloarRestClient createClient(RestClient.Builder restClientBuilder) {
        restClientBuilder.bufferContent((uri, method) -> true);
        restClientBuilder.requestInterceptor(new UpstreamLoggingInterceptor());
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClientBuilder.build()))
            .build();
        return proxyFactory.createClient(DragonsOfMugloarRestClient.class);
    }

    private static void expectRequest(MockRestServiceServer server, HttpMethod method, String path) {
        server.expect(requestTo(BASE_URL + path))
            .andExpect(method(method))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    }
}
