package at.gaderman.soulSnatcher.souls.config;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulRegistry;
import at.gaderman.soulSnatcher.souls.SoulType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.function.Function;

public final class ConfigOption<T> {
    private final String id;
    private final SoulType soulType;
    private final T defaultValue;
    private volatile T cached;
    private final ConfigReader<T> reader;
    private final Function<T, T> valueFunction;
    private final Function<T, String> displayFunction;

    @FunctionalInterface
    public interface ConfigReader<T> {
        T read(FileConfiguration config, String path, T def);
    }

    public ConfigOption(String id, SoulType soulType, T defaultValue, ConfigReader<T> reader){
       this(id, soulType, defaultValue, reader, value -> value);
    }

    public ConfigOption(String id, SoulType soulType, T defaultValue, ConfigReader<T> reader, Function<T, T> valueFunction){
        this(id, soulType, defaultValue, reader, valueFunction, Object::toString);
    }

    public ConfigOption(String id, SoulType soulType, T defaultValue, ConfigReader<T> reader, Function<T, T> valueFunction, Function<T, String> displayFunction){
        this.id = id;
        this.soulType = soulType;
        this.defaultValue = defaultValue;
        this.reader = reader;
        this.valueFunction = valueFunction;
        this.displayFunction = displayFunction;

        reloadFromConfig();
    }

    public void reloadFromConfig(){
        cached = valueFunction.apply(reader.read(SoulSnatcher.getSoulsConfig(), SoulRegistry.soulConfigPath(soulType) + "." + id, defaultValue));
    }

    public String id(){
        return id;
    }

    public T cached(){
        return cached;
    }

    public T defaultValue(){
        return defaultValue;
    }

    public String displayValue() {
        return displayFunction.apply(cached);
    }
}
