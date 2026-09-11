package at.gaderman.soulSnatcher.souls.config;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.config.ConfigOption;
import at.gaderman.soulSnatcher.config.ConfigOptionRegistry;
import at.gaderman.soulSnatcher.config.lang.LanguageKeyPlaceholderHolder;
import at.gaderman.soulSnatcher.souls.SoulRegistry;
import at.gaderman.soulSnatcher.souls.SoulType;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ConfigHoldingSoulType extends SoulType implements ExtraConfigHolder, LanguageKeyPlaceholderHolder {
    private final ConfigOptionRegistry configRegistry = new ConfigOptionRegistry(SoulSnatcher.getSoulsConfig(), SoulRegistry.soulConfigPath(this));

    protected <T> ConfigOption<T> configOption(String id, T defaultValue, ConfigOption.ConfigReader<T> reader) {
        return configRegistry.configOption(id, defaultValue, reader, value -> value);
    }

    protected <T> ConfigOption<T> configOption(String id, T defaultValue, ConfigOption.ConfigReader<T> reader, Function<T, T> valueFunction) {
        return configRegistry.configOption(id, defaultValue, reader, valueFunction);
    }

    protected <T> ConfigOption<T> configOption(String id, T defaultValue, ConfigOption.ConfigReader<T> reader, Function<T, T> valueFunction, Function<T, String> displayFunction) {
        return configRegistry.configOption(id, defaultValue, reader, valueFunction, displayFunction);
    }

    @Override
    public Map<String, Object> extraConfigPathValueMap() {
        return configRegistry.configOptions().values().stream()
                .sorted(Comparator.comparing(ConfigOption::id))
                .collect(Collectors.toMap(
                        ConfigOption::id,
                        ConfigOption::defaultValue,
                        (a, _) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, String> placeholderMap() {
        Map<String, String> placeholders = new LinkedHashMap<>();
        configRegistry.configOptions().values().forEach(option -> {
            placeholders.put(option.id(), option.displayValue());
        });
        return placeholders;
    }

    protected String fromMillisToSeconds(int millis){
        return String.valueOf(millis / 1000.0);
    }

    protected String fromTicksToSeconds(int ticks){
        return String.valueOf(ticks / 20.0);
    }

    protected String toPercentageString(double base){
        return String.valueOf(base * 100.0);
    }
}
