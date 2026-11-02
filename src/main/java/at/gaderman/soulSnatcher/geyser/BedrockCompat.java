package at.gaderman.soulSnatcher.geyser;

import at.gaderman.soulSnatcher.SoulSnatcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BedrockCompat {
    private static boolean geyser;

    public static void init(SoulSnatcher plugin) {
        geyser = Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot");
        if (geyser) {
            new GeyserHook().register(plugin);
            plugin.getLogger().info("Geyser detected, enabling Bedrock compatibility.");
        }
    }

    public static boolean isBedrockPlayer(Player player) {
        return geyser && GeyserHook.isBedrockPlayer(player.getUniqueId());
    }
}