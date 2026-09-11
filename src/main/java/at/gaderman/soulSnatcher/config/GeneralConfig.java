package at.gaderman.soulSnatcher.config;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.effects.SoulReward;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

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

            if(value.comment() != null)
                config.setComments(setKey, List.of(value.comment()));
        });

        configRegistry.reloadConfig();
        SoulSnatcher.getPlugin().saveConfig();
    }

    public final ConfigOption<Integer> MAX_BOUND_SOULS = configRegistry.configOption("souls.max_bound", SoulType.MAX_BOUND_SOULS, FileConfiguration::getInt, value -> Math.clamp(value, 1, 4))
            .withComment("The max amount of souls a player can bind. RECOMMENDED to leave at 2. 1 will tone down the plugin to slight flavor. 3-4 will break balance. (Max 4)");
    public final ConfigOption<Integer> MAX_UNBOUND_SOULS = configRegistry.configOption("souls.max_soul_pool", SoulType.MAX_UNBOUND_SOULS, FileConfiguration::getInt)
            .withComment("The max amount of unbound souls a player can carry, these are the souls which will infuse nearby spawned mobs");
    public final ConfigOption<Integer> SOUL_DESPAWN_TIME = configRegistry.configOption("souls.reward_despawn_time", SoulReward.LIVING_TICKS, FileConfiguration::getInt, value -> Math.max(value, 1))
            .withComment("The amount of time a soul reward stays in the world before vanishing when not absorbed in ticks (20 ticks = 1 second)");
    public final ConfigOption<Integer> VIAL_COOLDOWN = configRegistry.configOption("souls.vial_cooldown", 60 * 20, FileConfiguration::getInt, value -> Math.max(value, 1))
            .withComment("The amount of cooldown when using a soul vial in ticks (20 ticks = 1 second)");

    public final ConfigOption<Boolean> LOOSE_SOULS_ON_DEATH = configRegistry.configOption("souls.lose_souls_on_death", true, FileConfiguration::getBoolean)
            .withComment("Lose all souls on death, when killed by another player offer them as reward (KeepInventory will always save souls regardless of this setting)");

    public final ConfigOption<Double> INFUSION_XZ_RADIUS = configRegistry.configOption("infusion.xz-radius", 50.0, FileConfiguration::getDouble, value -> Math.max(0, value))
            .withComment("The xz distance which mobs have to be from a player to be valid infusion targets on spawn");
    public final ConfigOption<Double> INFUSION_Y_RADIUS = configRegistry.configOption("infusion.y-radius", 30.0, FileConfiguration::getDouble, value -> Math.max(0, value))
            .withComment("The y distance which mobs have to be from a player to be valid infusion targets on spawn");

    public final ConfigOption<Boolean> EXCLUDE_WORLDS = configRegistry.configOption("worlds.exclude_world_list", true, FileConfiguration::getBoolean)
            .withComment("When true the followed world list is for EXCLUSION (no soul mechanics work in the listed worlds), when false its INCLUSION (soul mechanics only work in listed worlds)");
    public final ConfigOption<List<String>> WORLD_LIST = configRegistry.configOption("worlds.list", List.of("no_souls", "hub_world", "fest_world"), (config, path, def) -> config.getStringList(path))
            .withComment("List of worlds to either exclude soul mechanism or be the only ones it works in");


}
