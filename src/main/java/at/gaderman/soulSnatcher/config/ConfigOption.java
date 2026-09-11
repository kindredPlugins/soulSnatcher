package at.gaderman.soulSnatcher.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public final class ConfigOption<T> {
    private final String id;
    private final YamlConfiguration config;
    private final T defaultValue;
    private volatile T cached;
    private final ConfigReader<T> reader;
    private final Function<T, T> valueFunction;
    private final Function<T, String> displayFunction;

    private @Nullable String comment;

    @FunctionalInterface
    public interface ConfigReader<T> {
        T read(FileConfiguration config, String path, T def);
    }

    public ConfigOption(String id, YamlConfiguration config, T defaultValue, ConfigReader<T> reader){
       this(id, config, defaultValue, reader, value -> value);
    }

    public ConfigOption(String id, YamlConfiguration config, T defaultValue, ConfigReader<T> reader, Function<T, T> valueFunction){
        this(id, config, defaultValue, reader, valueFunction, Object::toString);
    }

    public ConfigOption(String id, YamlConfiguration config, T defaultValue, ConfigReader<T> reader, Function<T, T> valueFunction, Function<T, String> displayFunction){
        this.id = id;
        this.config = config;
        this.defaultValue = defaultValue;
        this.reader = reader;
        this.valueFunction = valueFunction;
        this.displayFunction = displayFunction;

        reloadFromConfig();
    }

    public ConfigOption<T> withComment(String comment){
        this.comment = comment;
        config.setComments(id, List.of(comment));
        return this;
    }

    public void reloadFromConfig(){
        cached = valueFunction.apply(reader.read(config, id, defaultValue));
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

    public @Nullable String comment(){
        return comment;
    }
}
