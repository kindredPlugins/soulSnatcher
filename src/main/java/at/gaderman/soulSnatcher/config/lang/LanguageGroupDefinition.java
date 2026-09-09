package at.gaderman.soulSnatcher.config.lang;

import net.kyori.adventure.text.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//TODO: rather abundant using AutoService and this whole static facade, probably be better to just call into LanguageManager directly, sort of, careful of service loops though
/**
 * Represents a separate class which uses LanguageKeyHolder to register a group of language keys with default components
 */
public abstract class LanguageGroupDefinition implements LanguageKeyHolder{

    private static final Map<String, List<Component>> languageKeyDefaultMap = new LinkedHashMap<>();

    protected static LanguageKey registerKey(String key, Component ...components){
        languageKeyDefaultMap.put(key, List.of(components));
        return new LanguageKey(key);
    }

    @Override
    public Map<String, List<Component>> languageKeyDefaultMap() {
        return languageKeyDefaultMap;
    }
}
