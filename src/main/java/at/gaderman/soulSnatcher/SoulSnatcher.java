package at.gaderman.soulSnatcher;

import at.gaderman.soulSnatcher.commands.SoulIndexCommand;
import at.gaderman.soulSnatcher.commands.SoulLanternCommand;
import at.gaderman.soulSnatcher.config.GeneralConfig;
import at.gaderman.soulSnatcher.config.lang.LanguageManager;
import at.gaderman.soulSnatcher.souls.SoulListener;
import at.gaderman.soulSnatcher.souls.SoulRegistry;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.TriggerListener;
import at.gaderman.soulSnatcher.souls.items.SoulLanternManager;
import at.gaderman.soulSnatcher.souls.items.SoulVialManager;
import at.gaderman.soulSnatcher.utils.UpdateChecker;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SoulSnatcher extends JavaPlugin {

    public static SoulSnatcher getPlugin() {
        return SoulSnatcher.getPlugin(SoulSnatcher.class);
    }

    private static YamlConfiguration soulsConfig;

    @Override
    public void onEnable() {
        // Plugin startup logic
        GeneralConfig.getInstance();

        SoulRegistry.getInstance();
        LanguageManager.getInstance();

        Bukkit.getPluginManager().registerEvents(new SoulListener(), this);
        Bukkit.getPluginManager().registerEvents(new TriggerListener(), this);
        Bukkit.getPluginManager().registerEvents(new SoulLanternManager(), this);
        Bukkit.getPluginManager().registerEvents(new SoulVialManager(), this);

        getCommand("soulIndex").setExecutor(new SoulIndexCommand());
        getCommand("soulLantern").setExecutor(new SoulLanternCommand());

        new UpdateChecker(this).check();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.disabling = true;

        getLogger().info("Cleaning up soul instances of leftover players");
        Bukkit.getOnlinePlayers().forEach(player -> {
            SoulType.removeFromCache(player);
            player.saveData();
        });
        getLogger().info("Completed leftover players soul clean up");

        int taskSize = runningTasks.size();
        getLogger().info("Running down " + taskSize + " cleanup tasks");
        runningTasks.values().forEach(Runnable::run);
        runningTasks.clear();
        getLogger().info("Completed running cleanup tasks");
    }

    //region Shutdown

    private boolean disabling = false;

    private final Map<ScheduledTask, Runnable> runningTasks = new ConcurrentHashMap<>();

    /**
     * Registers a action that will be executed if the plugin is disabled. This is used when a server stop
     * would leave certain things in an unnatural state
     *
     * @param task   The task of the run Bukkit Task, this is used in a map to quickly access tasks
     * @param action The cleanup runnable that should be executed on disable
     */
    public void registerCleanUpTask(ScheduledTask task, Runnable action) {
        runningTasks.put(task, action);
    }

    /**
     * Registers the given runnable as a BukkitTask and runs it via the Bukkit Scheduler. Additionally, saves this
     * Task as a runningTask, meaning that if the plugin is disabled, this will get executed instantly instead.
     * This makes sure that certain actions do not remain in an unnatural state upon sudden server stop.
     * Moreover, after the delay this also unregisters itself meaning no memory leaks without further action externally
     *
     * @param runnable A runnable that should be delayed by the given delay
     * @param delay    How much delay the runnable should have int ticks
     * @return The BukkitTask that was created by delaying the Runnable via the Bukkit Scheduler
     */
    public ScheduledTask registerDelayedTask(Runnable runnable, long delay) {
        var scheduledTask = Bukkit.getGlobalRegionScheduler().runDelayed(this, task -> {
            runnable.run();
            unregisterCleanUpTask(task);
        }, delay);
        registerCleanUpTask(scheduledTask, runnable);
        return scheduledTask;
    }

    /**
     * Removes a cleanup task from the pool. This is used to keeping the runningTasks sizeable and not impact
     * performance too much
     */
    public void unregisterCleanUpTask(ScheduledTask task) {
        if (disabling) return;
        runningTasks.remove(task);
    }

    //endregion

    //region Config

    public static YamlConfiguration getSoulsConfig() {
        return soulsConfig;
    }

    public static void setSoulsConfig(YamlConfiguration config) {
        soulsConfig = config;
    }

    //endregion
}
