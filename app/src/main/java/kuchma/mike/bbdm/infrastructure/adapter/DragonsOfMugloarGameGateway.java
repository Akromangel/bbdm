package kuchma.mike.bbdm.infrastructure.adapter;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import kuchma.mike.bbdm.domain.model.GameId;
import kuchma.mike.bbdm.domain.model.GameMessage;
import kuchma.mike.bbdm.domain.model.ItemId;
import kuchma.mike.bbdm.domain.model.ItemPurchaseResult;
import kuchma.mike.bbdm.domain.model.MessageBoard;
import kuchma.mike.bbdm.domain.model.MessageId;
import kuchma.mike.bbdm.domain.model.MessageResolution;
import kuchma.mike.bbdm.domain.model.ShopInventory;
import kuchma.mike.bbdm.domain.model.ShopItem;
import kuchma.mike.bbdm.domain.model.ShopItemId;
import kuchma.mike.bbdm.domain.model.StartedGame;
import kuchma.mike.bbdm.domain.port.GameGateway;
import kuchma.mike.bbdm.application.MessageResolutionRejectedException;
import kuchma.mike.bbdm.infrastructure.config.MessageFilterProperties;
import kuchma.mike.bbdm.infrastructure.client.DragonsOfMugloarRestClient;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class DragonsOfMugloarGameGateway implements GameGateway {
    private final DragonsOfMugloarRestClient restClient;
    private final Set<String> filteredProbabilities;

    public DragonsOfMugloarGameGateway(
        final DragonsOfMugloarRestClient restClient,
        final MessageFilterProperties messageFilterProperties
    ) {
        this.restClient = restClient;
        this.filteredProbabilities = Set.copyOf(messageFilterProperties.filteredProbabilities());
    }

    @Override
    public StartedGame startGame() {
        return restClient.startGame();
    }

    @Override
    public MessageBoard getMessages(final GameId gameId) {
        List<GameMessage> messages = restClient.getMessages(gameId)
            .stream()
            .filter(message -> !hasFilteredProbability(message.probability()))
            .sorted(
                Comparator.comparingInt(GameMessage::expiresIn)
                    .thenComparing(Comparator.comparingInt(GameMessage::reward).reversed())
            )
            .toList();
        return new MessageBoard(messages);
    }

    private boolean hasFilteredProbability(final @Nullable String probability) {
        return probability != null && filteredProbabilities.contains(probability);
    }

    @Override
    public MessageResolution dealWithMessage(final GameId gameId, final MessageId messageId) {
        try {
            return restClient.dealWithMessage(gameId, messageId);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == BAD_REQUEST) {
                throw new MessageResolutionRejectedException(messageId.value());
            }
            throw exception;
        }
    }

    @Override
    public ShopInventory getShop(final GameId gameId) {
        List<ShopItem> items = restClient.getShop(gameId);
        return new ShopInventory(items);
    }

    @Override
    public ItemPurchaseResult buyItem(final GameId gameId, final ShopItemId itemId) {
        return restClient.buyItem(gameId, new ItemId(itemId.value()));
    }
}
