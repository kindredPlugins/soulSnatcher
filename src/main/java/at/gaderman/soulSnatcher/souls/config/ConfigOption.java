package at.gaderman.soulSnatcher.souls.config;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulRegistry;
import at.gaderman.soulSnatcher.souls.SoulType;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigOption<T> {
    private final String id;
    private final SoulType soulType;
    private final T defaultValue;
    private volatile T cached;
    private final ConfigReader<T> reader;

    @FunctionalInterface
    public interface ConfigReader<T> {
        T read(FileConfiguration config, String path, T def);
    }

    public ConfigOption(String id, SoulType soulType, T defaultValue, ConfigReader<T> reader){
        this.id = id;
        this.soulType = soulType;
        this.defaultValue = defaultValue;
        this.reader = reader;

        reloadFromConfig();
    }

    public void reloadFromConfig(){
        cached = reader.read(SoulSnatcher.getSoulsConfig(), SoulRegistry.soulConfigPath(soulType) + "." + id, defaultValue);
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
}
