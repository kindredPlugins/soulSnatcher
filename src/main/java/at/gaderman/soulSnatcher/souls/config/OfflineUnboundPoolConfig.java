package at.gaderman.soulSnatcher.souls.config;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class OfflineUnboundPoolConfig {

    private static OfflineUnboundPoolConfig instance;

    private static final String FILE_NAME = "offline_unbound_pool_cache.yml";
    private static final String PREFIX = "offlinePool.";

    private final File configFile;
    private final YamlConfiguration offlineBoundPoolConfig;

    private OfflineUnboundPoolConfig() {
        configFile = new File(SoulSnatcher.getPlugin().getDataFolder(), FILE_NAME);
        offlineBoundPoolConfig = configFile.exists()
                ? YamlConfiguration.loadConfiguration(configFile)
                : new YamlConfiguration();

        offlineBoundPoolConfig.setComments(PREFIX.substring(0, PREFIX.length() - 1), List.of("When a player is offline while a mob infused from his pool despawns",
                "it cannot be directly synced to the player anymore, this file is the media between",
                "You should not modify any data in here"));
    }

    public static OfflineUnboundPoolConfig getInstance() {
        if (instance == null) instance = new OfflineUnboundPoolConfig();
        return instance;
    }

    public List<String> offlinePoolOfPlayer(Player player) {
        return offlineBoundPoolConfig.getStringList(PREFIX + player.getUniqueId());
    }

    public void clearOfflinePoolPlayer(Player player) {
        offlineBoundPoolConfig.set(PREFIX + player.getUniqueId(), null);
        saveConfig();
    }

    public void addToOfflinePoolPlayer(String uuid, SoulType soulType) {
        List<String> pool = offlineBoundPoolConfig.getStringList(PREFIX + uuid);
        pool.add(soulType.id());
        offlineBoundPoolConfig.set(PREFIX + uuid, pool);

        saveConfig();
    }

    private void saveConfig() {
        try {
            offlineBoundPoolConfig.save(configFile);
        } catch (IOException e) {
            SoulSnatcher.getPlugin().getLogger().warning("Something happened to " + FILE_NAME + " it was no longer valid. Cannot access soul pools of offline players any longer.");
        }
    }
}
