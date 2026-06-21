package at.gaderman.soulSnatcher.souls.config;

import at.gaderman.soulSnatcher.souls.SoulType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ConfigHoldingSoulType extends SoulType implements ExtraConfigHolder {
    private final Map<String, ConfigOption<?>> configOptions = new LinkedHashMap<>();

    protected <T> ConfigOption<T> configOption(String id, T defaultValue, ConfigOption.ConfigReader<T> reader) {
        ConfigOption<T> option = new ConfigOption<>(id, this, defaultValue, reader);
        configOptions.put(id, option);
        return option;
    }

    public Map<String, ConfigOption<?>> configOptions() {
        return Collections.unmodifiableMap(configOptions);
    }

    public void reloadConfig() {
        configOptions.values().forEach(ConfigOption::reloadFromConfig);
    }

    @Override
    public Map<String, Object> extraConfigPathValueMap() {
        return configOptions.values().stream()
                .collect(Collectors.toMap(ConfigOption::id, ConfigOption::defaultValue));
    }
}
