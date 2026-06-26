package at.gaderman.soulSnatcher.souls.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.Map;

/**
 * A template which holds extra config information
 */
public interface ExtraConfigHolder {

    /**
     * Returns a map of config entries and their respective default value, this allows the config sync to add
     * predefined paths
     */
    Map<String, Object> extraConfigPathValueMap();

    /**
     * Returns a map of config entries and their respective comments, allowing predefined comments in the YAML
     * Optional method - returns empty map by default
     */
    default Map<String, String> extraConfigPathCommentMap() {
        return Collections.emptyMap();
    }

    /**
     * Used when a config holder has a unique serialization instead of the usual configMap
     */
    default void writeExtraConfigDefaults(YamlConfiguration config, String basePath) {}
}
