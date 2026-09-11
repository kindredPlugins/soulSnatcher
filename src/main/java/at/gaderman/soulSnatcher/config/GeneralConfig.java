package at.gaderman.soulSnatcher.config;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class GeneralConfig {

    private static GeneralConfig instance;

    private GeneralConfig() {
        setUp();
    }

    public static GeneralConfig getInstance() {
        if (instance == null) instance = new GeneralConfig();
        return instance;
    }

    private final YamlConfiguration config = (YamlConfiguration) SoulSnatcher.getPlugin().getConfig();
    private final ConfigOptionRegistry configRegistry = new ConfigOptionRegistry(config, "settings");

    private void setUp() {
        configRegistry.configOptions().forEach((key, value) -> {
            String setKey = "settings." + key;

            if (!config.contains(setKey))
                config.set(setKey, value.defaultValue());
        });

        configRegistry.reloadConfig();
        SoulSnatcher.getPlugin().saveConfig();
    }

    public final ConfigOption<Integer> MAX_UNBOUND_SOULS = configRegistry.configOption("max_soul_pool", SoulType.MAX_UNBOUND_SOULS, FileConfiguration::getInt)
            .withComment("The max amount of unbound souls a player can carry, these are the souls which will infuse nearby spawned mobs");

    public final ConfigOption<Boolean> LOOSE_SOULS_ON_DEATH = configRegistry.configOption("lose_souls_on_death", true, FileConfiguration::getBoolean)
            .withComment("Lose all souls on death, when killed by another player offer them as reward (KeepInventory will always save souls regardless of this setting)");

    public final ConfigOption<Double> INFUSION_XZ_RADIUS = configRegistry.configOption("infusion.xz-radius", 50.0, FileConfiguration::getDouble, value -> Math.max(0, value))
            .withComment("The xz distance which mobs have to be from a player to be valid infusion targets on spawn");
    public final ConfigOption<Double> INFUSION_Y_RADIUS = configRegistry.configOption("infusion.y-radius", 30.0, FileConfiguration::getDouble, value -> Math.max(0, value))
            .withComment("The y distance which mobs have to be from a player to be valid infusion targets on spawn");


}
