package kuchma.mike.bbdm.infrastructure.config;

import kuchma.mike.bbdm.application.GameService;
import kuchma.mike.bbdm.domain.port.GameGateway;
import kuchma.mike.bbdm.infrastructure.client.DragonsOfMugloarRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class GameApiConfiguration {
    @Bean
    GameService gameService(GameGateway gameGateway) {
        return new GameService(gameGateway);
    }

    @Bean
    DragonsOfMugloarRestClient dragonsOfMugloarRestClient(
        @Value("${dragonsofmugloar.api.base-url}") String baseUrl
    ) {
        RestClient restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();
        return proxyFactory.createClient(DragonsOfMugloarRestClient.class);
    }
}
