package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.config.ExtraConfigHolder;
import com.google.common.base.Stopwatch;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.StreamSupport;

public class SoulRegistry {

    private static SoulRegistry instance;

    public static SoulRegistry getInstance() {
        if (instance == null) instance = new SoulRegistry();
        return instance;
    }

    private final Map<String, SoulType> soulRegistry = new LinkedHashMap<>();
    private final Map<String, SoulType> legacySoulRegistry = new LinkedHashMap<>();

    private SoulRegistry() {
        SoulSnatcher.getPlugin().getLogger().info("Starting to read souls.yml");
        var stopwatch = Stopwatch.createStarted();

        syncSoulConfig();

        ServiceLoader<SoulType> loader = ServiceLoader.load(SoulType.class, getClass().getClassLoader());
        for (SoulType soul : loader) {
            register(soul);
        }

        SoulSnatcher.getPlugin().getLogger().info("Synced souls.yml done in " + stopwatch.elapsed().getNano() / 1000000 + "ms");
    }

    private void syncSoulConfig() {
        File file = new File(SoulSnatcher.getPlugin().getDataFolder(), "souls.yml");
        YamlConfiguration config = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();

        SoulSnatcher.setSoulsConfig(config);
        boolean dirty = false;

        ServiceLoader<SoulType> loader = ServiceLoader.load(SoulType.class, getClass().getClassLoader());
        for (SoulType soul : StreamSupport.stream(loader.spliterator(), false)
                .sorted(Comparator.comparing(SoulType::id))
                .toList()) {
            String path = soulConfigPath(soul);

            if (syncPath(config, path, soul))
                dirty = true;
        }

        if (dirty) {
            try {
                config.save(file);
            } catch (IOException e) {
                SoulSnatcher.getPlugin().getLogger().severe("Failed to sync souls.yml: " + e.getMessage());
            }
        }
    }

    private boolean syncPath(YamlConfiguration config, String path, SoulType soulType) {
        final boolean[] changeOccurred = {!config.contains(path)};

        String enabledPath = path + ".enabled";
        if (!config.contains(enabledPath))
            config.set(enabledPath, true);

        if (soulType instanceof ExtraConfigHolder extraConfigHolder) {
            Map<String, String> comments = extraConfigHolder.extraConfigPathCommentMap();

            extraConfigHolder.extraConfigPathValueMap().forEach((extraPath, value) -> {
                String completePath = path + "." + extraPath;

                if (config.contains(completePath)) return;
                config.set(completePath, value);

                if (comments.containsKey(extraPath)) {
                    config.setComments(completePath, Collections.singletonList(comments.get(extraPath)));
                }

                changeOccurred[0] = true;
            });

            extraConfigHolder.writeExtraConfigDefaults(config, path);
        }

        return changeOccurred[0];
    }

    private void register(SoulType soul) {
        String path = soulConfigPath(soul);
        boolean enabled = SoulSnatcher.getSoulsConfig().getBoolean(path + ".enabled", true);

        if (!enabled) {
            legacySoulRegistry.put(soul.id(), soul);
            SoulSnatcher.getPlugin().getLogger().info("Soul '" + soul.id() + "' is disabled in config, skipping.");
            return;
        }

        soulRegistry.put(soul.id(), soul);
    }

    public static String soulConfigPath(SoulType soul) {
        return "Souls." + soul.id();
    }

    public @Nullable SoulType getSoul(String id) {
        return soulRegistry.get(id);
    }

    public Optional<SoulType> getSoul(EntityType entityType) {
        return soulRegistry.values().stream().filter(soul -> soul.entityType().equals(entityType)).findFirst();
    }

    public Map<String, SoulType> soulRegistryMap() {
        return new LinkedHashMap<>(soulRegistry);
    }

    public Map<String, SoulType> legacySoulRegistryMap(){
        return new LinkedHashMap<>(legacySoulRegistry);
    }
}
