package at.gaderman.soulSnatcher.config.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//TODO: rather abundant using AutoService and this whole static facade, probably be better to just call into LanguageManager directly, sort of, careful of service loops though, would interfere with possible parameters in groupDefinitions though
/**
 * Represents a separate class which uses LanguageKeyHolder to register a group of language keys with default components
 */
public abstract class LanguageGroupDefinition implements LanguageKeyHolder{

    // store raw mini-message strings for defaults (either produced directly or serialized from Components)
    private static final Map<String, List<String>> languageKeyDefaultMap = new LinkedHashMap<>();

    /**
     * Register a key using raw mini-message strings. These strings will be deserialized by the LanguageManager.
     */
    protected static LanguageKey registerKey(String key, String ...components){
        languageKeyDefaultMap.put(key, List.of(components));
        return new LanguageKey(key);
    }

    /**
     * Compatibility helper: register using Components. Components will be serialized to mini-message strings and
     * stored as defaults.
     */
    protected static LanguageKey registerKey(String key, Component ...components){
        MiniMessage mm = MiniMessage.miniMessage();
        languageKeyDefaultMap.put(key, Arrays.stream(components).map(mm::serialize).toList());
        return new LanguageKey(key);
    }

    @Override
    public Map<String, List<Component>> languageKeyDefaultMap() {
        // deserialize stored raw mini-message strings into Components for consumers
        MiniMessage mm = MiniMessage.miniMessage();
        Map<String, List<Component>> out = new LinkedHashMap<>();
        languageKeyDefaultMap.forEach((k, v) -> out.put(k, v.stream().map(mm::deserialize).toList()));
        return out;
    }
}
