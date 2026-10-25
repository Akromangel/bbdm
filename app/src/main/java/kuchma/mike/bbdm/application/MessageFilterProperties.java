package kuchma.mike.bbdm.application;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dragonsofmugloar.messages")
public record MessageFilterProperties(
    List<String> filteredProbabilities,
    List<String> probabilityOrder
) {
    public MessageFilterProperties {
        filteredProbabilities = List.copyOf(filteredProbabilities);
        probabilityOrder = List.copyOf(probabilityOrder);
    }
}
