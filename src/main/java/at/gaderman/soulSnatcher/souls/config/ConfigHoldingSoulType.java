package at.gaderman.soulSnatcher.souls.config;

import at.gaderman.soulSnatcher.config.lang.LanguageKeyPlaceholderHolder;
import at.gaderman.soulSnatcher.souls.SoulType;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ConfigHoldingSoulType extends SoulType implements ExtraConfigHolder, LanguageKeyPlaceholderHolder {
    private final Map<String, ConfigOption<?>> configOptions = new LinkedHashMap<>();

    protected <T> ConfigOption<T> configOption(String id, T defaultValue, ConfigOption.ConfigReader<T> reader) {
        return configOption(id, defaultValue, reader, value -> value);
    }

    protected <T> ConfigOption<T> configOption(String id, T defaultValue, ConfigOption.ConfigReader<T> reader, Function<T, T> valueFunction) {
        return configOption(id, defaultValue, reader, valueFunction, Object::toString);
    }

    protected <T> ConfigOption<T> configOption(String id, T defaultValue, ConfigOption.ConfigReader<T> reader, Function<T, T> valueFunction, Function<T, String> displayFunction) {
        ConfigOption<T> option = new ConfigOption<>(id, this, defaultValue, reader, valueFunction, displayFunction);
        configOptions.put(id, option);
        return option;
    }

    public Map<String, ConfigOption<?>> configOptions() {
        return Collections.unmodifiableMap(configOptions);
    }

    public void clearConfigOptions(){
        configOptions.clear();
    }

    public void reloadConfig() {
        configOptions.values().forEach(ConfigOption::reloadFromConfig);
    }

    @Override
    public Map<String, Object> extraConfigPathValueMap() {
        return configOptions.values().stream()
                .sorted(Comparator.comparing(ConfigOption::id))
                .collect(Collectors.toMap(
                        ConfigOption::id,
                        ConfigOption::defaultValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, String> placeholderMap() {
        Map<String, String> placeholders = new LinkedHashMap<>();
        configOptions.forEach((id, option) -> {
            placeholders.put(id, option.displayValue());
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
