package kuchma.mike.bbdm.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

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

    private static DragonsOfMugloarRestClient createClient(RestClient.Builder restClientBuilder) {
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
