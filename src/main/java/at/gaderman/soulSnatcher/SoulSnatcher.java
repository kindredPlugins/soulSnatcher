package at.gaderman.soulSnatcher;

import at.gaderman.soulSnatcher.souls.SoulListener;
import at.gaderman.soulSnatcher.souls.SoulRegistry;
import at.gaderman.soulSnatcher.souls.TriggerListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulSnatcher extends JavaPlugin {

    public static Plugin getPlugin() { return SoulSnatcher.getPlugin(SoulSnatcher.class); }

    @Override
    public void onEnable() {
        // Plugin startup logic

        Bukkit.getPluginManager().registerEvents(new SoulListener(), this);
        Bukkit.getPluginManager().registerEvents(new TriggerListener(), this);

        SoulRegistry.getInstance();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
