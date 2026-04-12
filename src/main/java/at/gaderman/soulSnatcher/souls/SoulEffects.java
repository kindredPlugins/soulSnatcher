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

import java.util.*;

public class SoulEffects {
    private SoulEffects() {}

    /**
     * Holds all the mutable state for one entity's soul orbit.
     * Keeping it in a record makes it easy to pass around and reason about
     * without polluting the class with parallel maps.
     */
    private record OrbitState(
            UUID owner,
            List<SoulType> soulTypes,
            List<ItemDisplay> displays,
            double[] currentAngle,
            int[] tickCounter,
            BukkitTask task
    ) {}

    // One entry per entity that currently has souls orbiting it
    private static final Map<UUID, OrbitState> activeOrbits = new HashMap<>();

    // --- Constants ---

    private static final int DEFAULT_REVOLUTION_TICKS = 80;
    private static final double ORBIT_RADIUS = 1.2;
    private static final double HEIGHT_OFFSET = 1.1;
    private static final float HEAD_SCALE = 0.75f;
    private static final int PARTICLE_TICK = 3;

    // --- Public API ---

    /**
     * Adds one more soul to the orbit around the target entity.
     * If no orbit exists yet, a new task is started.
     * If one already exists, the new display is simply appended and
     * the angular offsets rebalance automatically on the next tick.
     */
    public static void addSoulToOrbit(Entity target, SoulType soulType) {
        var plugin = SoulSnatcher.getPlugin();

        OrbitState state = activeOrbits.get(target.getUniqueId());

        if (state == null) {
            // First soul — bootstrap the orbit from scratch
            List<ItemDisplay> displays = new ArrayList<>();
            displays.add(spawnDisplay(target, soulType));

            List<SoulType> soulTypes = new ArrayList<>();
            soulTypes.add(soulType);

            double[] angle = {0.0};
            int[] tickCounter = {0};
            final double angleStep = 360.0 / DEFAULT_REVOLUTION_TICKS;

            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    OrbitState current = activeOrbits.get(target.getUniqueId());

                    // Stop if the entity is gone or all displays were removed
                    if (!target.isValid() || target.isDead() || current == null) {
                        stopAllSoulOrbits(target);
                        cancel();
                        return;
                    }

                    Location center = target.getLocation().clone().add(0, HEIGHT_OFFSET, 0);
                    List<ItemDisplay> souls = current.displays();
                    int count = souls.size();

                    current.tickCounter()[0]++;
                    boolean particleTick = current.tickCounter[0] % PARTICLE_TICK == 0;

                    for (int i = 0; i < count; i++) {
                        ItemDisplay display = souls.get(i);

                        if (!display.isValid()) continue;

                        // Each soul is evenly spaced: soul i sits at its own
                        // angular offset of (i / count) * 360° ahead of the base angle
                        double soulAngle = (angle[0] + (360.0 / count) * i) % 360.0;
                        Location headPos = orbitPosition(center, soulAngle);

                        display.setInterpolationDelay(-1);
                        display.teleport(headPos);

                        if(particleTick)
                            spawnTrailParticles(current, center, soulAngle, angleStep);
                    }

                    angle[0] = (angle[0] + angleStep) % 360.0;
                }
            }.runTaskTimer(plugin, 0L, 1L);

            OrbitState newState = new OrbitState(target.getUniqueId(), soulTypes, displays, angle, tickCounter, task);
            activeOrbits.put(target.getUniqueId(), newState);
            plugin.registerCleanUpTask(task.getTaskId(), () -> stopAllSoulOrbits(target));

        } else {
            state.soulTypes().add(soulType);
            state.displays().add(spawnDisplay(target, soulType));
        }
    }

    /**
     * Removes one soul from the orbit (the most recently added one).
     * If that was the last soul, the task is cancelled and cleaned up.
     */
    public static void removeOneSoulFromOrbit(Entity target, SoulType soulType) {
        OrbitState state = activeOrbits.get(target.getUniqueId());
        if (state == null) return;

        for (int i = 0; i < state.soulTypes.size(); i++) {
            SoulType stateSoul = state.soulTypes.get(i);
            if(!stateSoul.equals(soulType)) continue;

            state.soulTypes.remove(i);
            ItemDisplay display = state.displays.remove(i);
            display.remove();
            break;
        }

        // If that was the last soul, tear down the whole orbit
        if (state.soulTypes.isEmpty()) {
            stopAllSoulOrbits(target);
        }
    }

    /**
     * Stops and cleans up every soul orbiting the given entity.
     */
    public static void stopAllSoulOrbits(Entity target) {
        OrbitState state = activeOrbits.remove(target.getUniqueId());
        if (state == null) return;

        state.task().cancel();
        SoulSnatcher.getPlugin().unregisterCleanUpTask(state.task().getTaskId());
        state.displays().forEach(Entity::remove);
    }

    private static final Set<UUID> playerViewingOrbits = new LinkedHashSet<>();

    /**
     * Marks a player to show them their own soul orbits
     * @param player The player who will be able to see their own orbit
     */
    public static void showSoulOrbits(Player player){
        OrbitState state = activeOrbits.getOrDefault(player.getUniqueId(), null);
        if(state == null) return;

        playerViewingOrbits.add(player.getUniqueId());
        state.displays.forEach(display -> player.showEntity(SoulSnatcher.getPlugin(), display));

        player.playSound(player, Sound.ITEM_FIRECHARGE_USE, 1f, 1.5f);
    }

    /**
     * Makes the soul orbits of the player go into hiding again
     * @param player The player who hides his soul orbits from themselves
     */
    public static void hideSoulOrbits(Player player){
        OrbitState state = activeOrbits.getOrDefault(player.getUniqueId(), null);
        if(state == null) return;

        playerViewingOrbits.remove(player.getUniqueId());
        state.displays.forEach(display -> player.hideEntity(SoulSnatcher.getPlugin(), display));

        player.playSound(player, Sound.ITEM_FIRECHARGE_USE, 1f, 0.5f);
    }

    /**
     * Returns how many souls are currently orbiting the given entity.
     * Useful for capping the maximum number of bound souls.
     */
    public static int getSoulCount(Entity target) {
        OrbitState state = activeOrbits.get(target.getUniqueId());
        return state == null ? 0 : state.displays().size();
    }

    public static boolean hasActiveSoulOrbit(Entity target) {
        return activeOrbits.containsKey(target.getUniqueId());
    }

    public static void playBindEffect(Player player, Location rewardLoc) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.5f);

        Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
            player.getWorld().playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.8f);
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                    player.getLocation().toCenterLocation(), 50, 0.2, 0.2, 0.2, 0.2);
        }, 20L);
    }

    //HELPERS
    private static ItemDisplay spawnDisplay(Entity target, SoulType soulType) {
        Location spawnLoc = target.getLocation().clone().add(0, HEIGHT_OFFSET, 0);

        return target.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
            d.setItemStack(soulType.getRepresentativeSkull());
            d.setBillboard(Display.Billboard.VERTICAL);
            d.setInterpolationDuration(3);
            d.setInterpolationDelay(1);
            d.setViewRange(1.0f);
            d.setTransformationMatrix(new Matrix4f()
                    .scale(HEAD_SCALE)
                    .rotateY((float) Math.toRadians(180)));

            if(target instanceof Player player)
                player.hideEntity(SoulSnatcher.getPlugin(), d);
        });
    }

    private static Location orbitPosition(Location center, double angleDegrees) {
        double radians = Math.toRadians(angleDegrees);
        double x = center.getX() + ORBIT_RADIUS * Math.cos(radians);
        double z = center.getZ() + ORBIT_RADIUS * Math.sin(radians);
        return new Location(center.getWorld(), x, center.getY(), z);
    }

    private static void spawnTrailParticles(OrbitState state, Location center, double currentAngle, double angleStep) {
        Location trailPos = orbitPosition(center, currentAngle - (angleStep * 2));
        Location headPos  = orbitPosition(center, currentAngle);

        boolean isPlayer = Bukkit.getPlayer(state.owner) != null && !playerViewingOrbits.contains(state.owner);

        center.getWorld().getPlayersSeeingChunk(center.getChunk()).forEach(player -> {
            if(isPlayer && player.getUniqueId().equals(state.owner)) return;

            player.spawnParticle(Particle.SOUL, trailPos,
                    1, 0.05, 0.05, 0.05, 0.01);
            player.spawnParticle(Particle.SOUL_FIRE_FLAME, headPos,
                    1, 0.03, 0.03, 0.03, 0.005);
        });

//        center.getWorld().spawnParticle(Particle.SOUL, trailPos,
//                1, 0.05, 0.05, 0.05, 0.01);
//        center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, headPos,
//                1, 0.03, 0.03, 0.03, 0.005);
    }
}
