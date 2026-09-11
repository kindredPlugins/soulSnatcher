package at.gaderman.soulSnatcher.config.lang;

import java.util.Map;

/**
 * Extension of LanguageKeyHolder which also features placeholders represented as "{placeholder}" within keys in order to input
 * certain values like config settings
 */
public interface LanguageKeyPlaceholderHolder extends LanguageKeyHolder {

    /**
     * Provides a map featuring all placeholder ids and their resolved value to input
     */
    Map<String, String> placeholderMap();

    default String wrapPlaceholder(String key){
        return "{" + key + "}";
    }
}
