package at.gaderman.soulSnatcher.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UpdateChecker {

    private static final String PROJECT_ID = "soulSnatcher";
    private static final String MODRINTH_API = "https://api.modrinth.com/v2";

    private final JavaPlugin plugin;
    private final String currentVersion;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    /**
     * Runs an async update check. Notifies ops on the main thread if a new version is found.
     */
    public void check() {
        // Get the server's MC version (e.g. "1.21.1")
        String mcVersion = Bukkit.getBukkitVersion().split("-")[0];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String url = MODRINTH_API + "/project/" + PROJECT_ID + "/version"
                        + "?game_versions=[%22" + mcVersion + "%22]"
                        + "&loaders=[%22paper%22]";
                if(url.contains(".build"))
                    url = url.split(".build")[0];

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        // Modrinth requires a descriptive User-Agent
                        .header("User-Agent", plugin.getName() + "/" + currentVersion
                                + "SoulSnatcher")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    plugin.getLogger().warning("[UpdateChecker] Modrinth returned HTTP "
                            + response.statusCode());
                    return;
                }

                JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();

                if (versions.isEmpty()) {
                    plugin.getLogger().info("[UpdateChecker] No versions found for MC " + mcVersion);
                    return;
                }

                JsonObject latest = versions.get(0).getAsJsonObject();
                String latestVersion = latest.get("version_number").getAsString();
                String projectUrl = "https://modrinth.com/plugin/" + PROJECT_ID;

                if (isNewer(latestVersion, currentVersion)) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            notifyOps(latestVersion, projectUrl));
                } else {
                    plugin.getLogger().info("[UpdateChecker] Plugin is up to date (" + currentVersion + ")");
                }

            } catch (Exception e) {
                plugin.getLogger().warning("[UpdateChecker] Update check failed: " + e.getMessage());
            }
        });
    }

    /**
     * Sends a clickable update notification to all online ops.
     */
    private void notifyOps(String latestVersion, String projectUrl) {
        Component message = Component.text()
                .append(Component.text("[" + plugin.getName() + "] ", NamedTextColor.GOLD))
                .append(Component.text("A new version is available! ", NamedTextColor.YELLOW))
                .append(Component.text(currentVersion, NamedTextColor.RED))
                .append(Component.text(" → ", NamedTextColor.GRAY))
                .append(Component.text(latestVersion, NamedTextColor.GREEN))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("[Download]", NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl(projectUrl)))
                .build();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(message);
            }
        }
        plugin.getLogger().warning("New version available: " + latestVersion
                + " (running " + currentVersion + ") - " + projectUrl);
    }

    /**
     * Simple semantic version comparison: returns true if `remote` is newer than `local`.
     * Handles versions like "1.4.2", "2.0", "1.0.0-SNAPSHOT", etc.
     */
    private boolean isNewer(String remote, String local) {
        int[] r = parseVersion(remote);
        int[] l = parseVersion(local);
        int len = Math.max(r.length, l.length);
        for (int i = 0; i < len; i++) {
            int rv = i < r.length ? r[i] : 0;
            int lv = i < l.length ? l[i] : 0;
            if (rv != lv) return rv > lv;
        }
        return false;
    }

    private int[] parseVersion(String version) {
        // Strip any build metadata / pre-release suffix (e.g. "-SNAPSHOT", "-beta.1")
        String clean = version.split("[-+]")[0];
        String[] parts = clean.split("\\.");
        int[] nums = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { nums[i] = Integer.parseInt(parts[i]); }
            catch (NumberFormatException e) { nums[i] = 0; }
        }
        return nums;
    }
}
