package kuchma.mike.bbdm.infrastructure.config;

import kuchma.mike.bbdm.application.GameService;
import kuchma.mike.bbdm.application.MessageFilterProperties;
import kuchma.mike.bbdm.application.ScriptingAdventureProperties;
import kuchma.mike.bbdm.domain.port.GameGateway;
import kuchma.mike.bbdm.infrastructure.client.DragonsOfMugloarRestClient;
import kuchma.mike.bbdm.infrastructure.client.GameOverInterceptor;
import kuchma.mike.bbdm.infrastructure.client.UpstreamLoggingInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableAsync
@EnableConfigurationProperties({MessageFilterProperties.class, ScriptingAdventureProperties.class})
public class GameApiConfiguration {
    private static final Logger upstreamLog = LoggerFactory.getLogger(UpstreamLoggingInterceptor.class);

    @Bean
    GameService gameService(GameGateway gameGateway) {
        return new GameService(gameGateway);
    }

    @Bean(name = "scriptingAdventureExecutor")
    ThreadPoolTaskExecutor scriptingAdventureExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("scripting-adventure-");
        return executor;
    }

    @Bean
    DragonsOfMugloarRestClient dragonsOfMugloarRestClient(
        @Value("${dragonsofmugloar.api.base-url}") String baseUrl
    ) {
        RestClient restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .bufferContent((_, _) -> upstreamLog.isDebugEnabled())
            .requestInterceptor(new GameOverInterceptor())
            .requestInterceptor(new UpstreamLoggingInterceptor())
            .build();
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();
        return proxyFactory.createClient(DragonsOfMugloarRestClient.class);
    }
}
