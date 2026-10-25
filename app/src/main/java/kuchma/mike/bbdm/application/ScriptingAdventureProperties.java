package kuchma.mike.bbdm.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dragonsofmugloar.scripting-adventure")
public record ScriptingAdventureProperties(
    int lastLife,
    int noLives,
    int failedMissionsBeforeUpgrades,
    int upgradeItemsToBuy
) {
}
