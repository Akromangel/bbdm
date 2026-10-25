package kuchma.mike.bbdm.infrastructure.client;

import kuchma.mike.bbdm.application.GameOverException;
import kuchma.mike.bbdm.application.MessageResolutionRejectedException;
import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.GameMessage;
import kuchma.mike.bbdm.domain.model.ItemId;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.MessageResolution;
import kuchma.mike.bbdm.domain.model.Reputation;
import kuchma.mike.bbdm.domain.model.ShopItem;
import kuchma.mike.bbdm.domain.model.StartedGame;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DragonsOfMugloarRestClientTest {
    private static final String GAME_ID = "game-1";

    private static final String START_GAME_PATH = "/api/v2/game/start";

    private static final String START_GAME_RESPONSE =
        """
            {
              "gameId": "game-1",
              "lives": 3,
              "gold": 10,
              "level": 1,
              "score": 0,
              "highScore": 0,
              "turn": 0
            }
            """;

    private WireMockServer wireMockServer;

    @BeforeEach
    void startWireMockServer() {
        wireMockServer = new WireMockServer(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig()
            .dynamicPort());
        wireMockServer.start();
    }

    @AfterEach
    void stopWireMockServer() {
        wireMockServer.stop();
    }

    @Test
    void shouldDeserializeTheCreatedGameResponse() {
        wireMockServer.stubFor(post(urlEqualTo(START_GAME_PATH)).willReturn(okJson(START_GAME_RESPONSE)));

        final DragonsOfMugloarRestClient client = createClient();

        assertThat(client.startGame())
            .satisfies(_ -> wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v2/game/start"))))
            .returns(GAME_ID, StartedGame::gameId)
            .returns(3, StartedGame::lives)
            .returns(10L, StartedGame::gold)
            .returns(1, StartedGame::level)
            .returns(0, StartedGame::score)
            .returns(0, StartedGame::highScore)
            .returns(0, StartedGame::turn);
    }

    @Test
    void shouldDeserializeTheReputationResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v2/" + GAME_ID + "/investigate/reputation"))
            .willReturn(okJson("""
                {
                  "people": 0.5,
                  "state": 0.4,
                  "underworld": 0.3
                }
                """)));

        final DragonsOfMugloarRestClient client = createClient();

        assertThat(client.getReputation(new GameId(GAME_ID)))
            .satisfies(_ -> wireMockServer.verify(
                getRequestedFor(urlEqualTo("/api/v2/" + GAME_ID + "/investigate/reputation"))))
            .returns(0.5, Reputation::people)
            .returns(0.4, Reputation::state)
            .returns(0.3, Reputation::underworld);
    }

    @Test
    void shouldDeserializeTheMessageBoardResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v2/" + GAME_ID + "/messages"))
            .willReturn(okJson(
                """
                    [
                      {
                        "adId": "ad-1",
                        "message": "Task",
                        "reward": 10,
                        "expiresIn": 3,
                        "probability": "75%"
                      },
                      {
                        "adId": "ad-2",
                        "message": "Another task",
                        "reward": 5,
                        "expiresIn": 4
                      }
                    ]
                    """
            )));

        final DragonsOfMugloarRestClient client = createClient();

        assertThat(client.getMessages(new GameId(GAME_ID)))
            .satisfies(_ -> wireMockServer.verify(getRequestedFor(urlEqualTo("/api/v2/" + GAME_ID + "/messages"))))
            .containsExactly(
                new GameMessage(new MessageId("ad-1"), "Task", 10, 3, "75%", 0),
                new GameMessage(new MessageId("ad-2"), "Another task", 5, 4, null, 0)
            );
    }

    @Test
    void shouldDecodeEncryptedMessageFields() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v2/" + GAME_ID + "/messages"))
            .willReturn(okJson("""
                [
                  {
                    "adId": "WWYyUzMyMTY=",
                    "message": "SW5maWx0cmF0ZSBUaGUgRWJvbnkgQ3Jvc3NlcyBhbmQgcmVjb3ZlciB0aGVpciBzZWNyZXRzLg==",
                    "reward": 151,
                    "expiresIn": 2,
                    "encrypted": 1,
                    "probability": "U3VpY2lkZSBtaXNzaW9u"
                  }
                ]
                """)));

        final DragonsOfMugloarRestClient client = createClient();

        assertThat(client.getMessages(new GameId(GAME_ID)))
            .singleElement()
            .returns(new MessageId("Yf2S3216"), GameMessage::adId)
            .returns("Infiltrate The Ebony Crosses and recover their secrets.", GameMessage::message)
            .returns("Suicide mission", GameMessage::probability)
            .returns(1, GameMessage::encrypted);
    }

    @Test
    void shouldSerializeTheDecisionAndDeserializeTheReply() {
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/" + GAME_ID + "/solve/ad-1"))
            .willReturn(okJson(
                """
                    {
                      "success": true,
                      "lives": 3,
                      "gold": 20,
                      "score": 10,
                      "highScore": 10,
                      "turn": 1,
                      "message": "Solved"
                    }
                    """
            )));

        final DragonsOfMugloarRestClient client = createClient();

        assertThat(client.dealWithMessage(new GameId(GAME_ID), new MessageId("ad-1")))
            .satisfies(_ -> wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v2/" + GAME_ID + "/solve/ad-1"))
                .withRequestBody(absent())))
            .returns(true, MessageResolution::success)
            .returns(3, MessageResolution::lives)
            .returns(20L, MessageResolution::gold)
            .returns(10, MessageResolution::score)
            .returns(10, MessageResolution::highScore)
            .returns(1, MessageResolution::turn)
            .returns("Solved", MessageResolution::message);
    }

    @Test
    void shouldDeserializeTheInventoryResponse() {
        final String shopResponse =
            """
                [
                  {
                    "id": "item-1",
                    "name": "Potion",
                    "cost": 5
                  }
                ]
                """;
        wireMockServer.stubFor(get(urlEqualTo("/api/v2/" + GAME_ID + "/shop"))
            .willReturn(okJson(shopResponse)));

        final DragonsOfMugloarRestClient client = createClient();

        assertThat(client.getShop(new GameId(GAME_ID)))
            .satisfies(_ -> wireMockServer.verify(getRequestedFor(urlEqualTo("/api/v2/" + GAME_ID + "/shop"))))
            .containsExactly(new ShopItem(new ItemId("item-1"), "Potion", 5));
    }

    @Test
    void shouldTreatNotFoundAsGameOverForShopRequests() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v2/" + GAME_ID + "/shop"))
            .willReturn(aResponse().withStatus(404)));

        final DragonsOfMugloarRestClient client = createClient();

        assertThatThrownBy(() -> client.getShop(new GameId(GAME_ID)))
            .isInstanceOf(GameOverException.class);
    }

    @Test
    void shouldTreatGoneAsGameOverForMessageRequests() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v2/" + GAME_ID + "/messages"))
            .willReturn(aResponse().withStatus(410)));

        final DragonsOfMugloarRestClient client = createClient();

        assertThatThrownBy(() -> client.getMessages(new GameId(GAME_ID)))
            .isInstanceOf(GameOverException.class);
    }

    @Test
    void shouldTreatBadRequestWhileResolvingMessageAsRejected() {
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/" + GAME_ID + "/solve/ad-1"))
            .willReturn(aResponse().withStatus(400)));

        final DragonsOfMugloarRestClient client = createClient();

        assertThatThrownBy(() -> client.dealWithMessage(new GameId(GAME_ID), new MessageId("ad-1")))
            .isInstanceOfSatisfying(
                MessageResolutionRejectedException.class,
                exception -> assertThat(exception.messageId()).isEqualTo("ad-1")
            );
    }

    @Test
    void shouldDeserializeThePurchaseResultResponse() {
        final String purchaseResponse =
            """
                {
                  "shoppingSuccess": true,
                  "gold": 5,
                  "lives": 3,
                  "level": 1,
                  "turn": 2
                }
                """;
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/" + GAME_ID + "/shop/buy/item-1"))
            .willReturn(okJson(purchaseResponse)));

        final DragonsOfMugloarRestClient client = createClient();

        assertThat(client.buyItem(new GameId(GAME_ID), new ItemId("item-1")))
            .satisfies(_ -> wireMockServer.verify(
                postRequestedFor(urlEqualTo("/api/v2/" + GAME_ID + "/shop/buy/item-1"))))
            .returns(true, ItemPurchaseResult::shoppingSuccess)
            .returns(5L, ItemPurchaseResult::gold)
            .returns(3, ItemPurchaseResult::lives)
            .returns(1, ItemPurchaseResult::level)
            .returns(2, ItemPurchaseResult::turn);
    }

    @Test
    void shouldKeepResponseBodyReadableWhenDebugLoggingIsEnabled() {
        final Logger logger = (Logger) LoggerFactory.getLogger(UpstreamLoggingInterceptor.class);
        final Level previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);

        try {
            wireMockServer.stubFor(post(urlEqualTo(START_GAME_PATH)).willReturn(okJson(START_GAME_RESPONSE)));

            final StartedGame response = createClient().startGame();

            assertThat(response.gameId())
                .satisfies(_ -> wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v2/game/start"))))
                .isEqualTo(GAME_ID);
        } finally {
            logger.setLevel(previousLevel);
        }
    }

    private DragonsOfMugloarRestClient createClient() {
        final RestClient restClient = RestClient.builder()
            .baseUrl(wireMockServer.baseUrl())
            .bufferContent((_, _) -> true)
            .requestInterceptor(new GameOverInterceptor())
            .requestInterceptor(new UpstreamLoggingInterceptor())
            .build();
        final HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();
        return proxyFactory.createClient(DragonsOfMugloarRestClient.class);
    }
}
