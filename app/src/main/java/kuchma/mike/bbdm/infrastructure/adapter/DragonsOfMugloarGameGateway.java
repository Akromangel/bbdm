package kuchma.mike.bbdm.infrastructure.adapter;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.MessageDecision;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.ShopItemId;
import kuchma.mike.bbdm.domain.port.GameGateway;
import kuchma.mike.bbdm.infrastructure.client.DragonsOfMugloarRestClient;
import org.springframework.stereotype.Component;

@Component
public class DragonsOfMugloarGameGateway implements GameGateway {
    private final DragonsOfMugloarRestClient restClient;

    public DragonsOfMugloarGameGateway(DragonsOfMugloarRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Object startGame() {
        return restClient.startGame();
    }

    @Override
    public Object getMessages(GameId gameId) {
        return restClient.getMessages(gameId.value());
    }

    @Override
    public Object dealWithMessage(GameId gameId, MessageId messageId, MessageDecision decision) {
        return restClient.dealWithMessage(
            gameId.value(),
            messageId.value(),
            new DragonsOfMugloarRestClient.DealMessageRequest(decision.toUpstreamValue())
        );
    }

    @Override
    public Object getShop(GameId gameId) {
        return restClient.getShop(gameId.value());
    }

    @Override
    public Object buyItem(GameId gameId, ShopItemId itemId) {
        return restClient.buyItem(gameId.value(), itemId.value());
    }
}
