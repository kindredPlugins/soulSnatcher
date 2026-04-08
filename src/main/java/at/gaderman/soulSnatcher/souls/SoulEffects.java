package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.SoulSnatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoulEffects {
    private SoulEffects() {}

    private static final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private static final Map<UUID, ItemDisplay> activeDisplays = new HashMap<>();

    private static final int DEFAULT_REVOLUTIONS_TRICKS = 80;
    private static final double ORBIT_RADIUS = 1.2;
    private static final double HEIGHT_OFFSET = 1.1;
    private static final float HEAD_SCALE = 0.75f;

    public static void startSoulOrbit(Entity target, SoulType soulType) {
        var plugin = SoulSnatcher.getPlugin();

        stopSoulOrbit(target);

        final double angleStep = 360.0 / DEFAULT_REVOLUTIONS_TRICKS;
        final double[] currentAngle = {0.0};

        ItemDisplay display = spawnReleaseSoulDisplay(target, soulType);
        activeDisplays.put(target.getUniqueId(), display);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isValid() || target.isDead() || !display.isValid()) {
                    stopSoulOrbit(target);
                    cancel();
                    return;
                }

                Location center = target.getLocation().clone().add(0, HEIGHT_OFFSET, 0);
                Location headPos = orbitPosition(center, currentAngle[0]);

                display.setInterpolationDelay(-1);
                display.teleport(headPos);

                spawnTrailParticles(center, currentAngle[0], angleStep);

                currentAngle[0] = (currentAngle[0] + angleStep) % 360.0;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeTasks.put(target.getUniqueId(), task);
        plugin.registerCleanUpTask(task.getTaskId(), () -> stopSoulOrbit(target));
    }

    public static void stopSoulOrbit(Entity target) {
        BukkitTask task = activeTasks.remove(target.getUniqueId());
        if (task != null){
            task.cancel();
            SoulSnatcher.getPlugin().unregisterCleanUpTask(task.getTaskId());
        }

        ItemDisplay display = activeDisplays.remove(target.getUniqueId());
        if (display != null) display.remove();
    }

    public static boolean hasActiveSoulOrbit(Entity target) {
        return activeTasks.containsKey(target.getUniqueId());
    }

    // --- Private helpers ---

    private static ItemDisplay spawnReleaseSoulDisplay(Entity target, SoulType soulType) {
        Location spawnLoc = target.getLocation().clone().add(0, HEIGHT_OFFSET, 0);

        return target.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
            d.setItemStack(soulType.getRepresentativeSkull());
            d.setBillboard(Display.Billboard.VERTICAL);
            d.setInterpolationDuration(2);
            d.setInterpolationDelay(0);
            d.setViewRange(1.0f);
            d.setTransformationMatrix(new Matrix4f().scale(HEAD_SCALE));
        });
    }

    private static Location orbitPosition(Location center, double angleDegrees) {
        double radians = Math.toRadians(angleDegrees);
        double x = center.getX() + ORBIT_RADIUS * Math.cos(radians);
        double z = center.getZ() + ORBIT_RADIUS * Math.sin(radians);
        return new Location(center.getWorld(), x, center.getY(), z);
    }

    private static void spawnTrailParticles(Location center, double currentAngle, double angleStep) {
        Location trailPos = orbitPosition(center, currentAngle - (angleStep * 2));
        Location headPos  = orbitPosition(center, currentAngle);

        center.getWorld().spawnParticle(Particle.SOUL, trailPos,
                1, 0.05, 0.05, 0.05, 0.01);
        center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, headPos,
                1, 0.03, 0.03, 0.03, 0.005);
    }

    public static void playBindEffect(Player player, Location rewardLoc){
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.5f);

        Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
            player.getWorld().playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.8f);
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().toCenterLocation(), 50, 0.2, 0.2, 0.2, 0.2);
        }, 20L);
    }
}
