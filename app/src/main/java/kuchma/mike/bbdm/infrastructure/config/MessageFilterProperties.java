package kuchma.mike.bbdm.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dragonsofmugloar.messages")
public record MessageFilterProperties(List<String> filteredProbabilities) {
}
