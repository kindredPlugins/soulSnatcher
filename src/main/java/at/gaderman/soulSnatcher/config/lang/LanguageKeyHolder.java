package at.gaderman.soulSnatcher.config.lang;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;

/**
 * Used by classes defining language keys, exposes a clean method to provide one or more language keys to register on startup
 */
public interface LanguageKeyHolder {

    /**
     * Provides a map of all language keys introduced by this class and their corresponding default component value
     */
    Map<String, List<Component>> languageKeyDefaultMap();

}
