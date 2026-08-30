package at.gaderman.soulSnatcher.souls.effects;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulType;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.*;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.security.auth.callback.Callback;
import java.util.*;
import java.util.function.Consumer;

public class SoulEffects {
    private SoulEffects() {
    }

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
    ) {
    }

    // One entry per entity that currently has souls orbiting it
    private static final Map<UUID, OrbitState> activeOrbits = new HashMap<>();

    //region Constants

    private static final int DEFAULT_REVOLUTION_TICKS = 80;
    private static final double ORBIT_RADIUS = 1.2;
    private static final double HEIGHT_OFFSET = 1.1;
    private static final float HEAD_SCALE = 0.75f;
    private static final int PARTICLE_TICK = 3;

    //endregion

    //region Soul Release

    // --- Released Soul Animation Constants ---
    private static final float RELEASED_HEAD_SCALE = 1f;
    private static final float RELEASED_ROTATION_SPEED = 16f;   // degrees per tick
    private static final double RELEASED_FLOAT_SPEED = 0.055; // blocks per tick
    private static final long RELEASED_LIFETIME_TICKS = 55L;  // how long before removal
    private static final int RELEASED_PARTICLE_INTERVAL = 2;    // spawn particles every N ticks

    public static void spawnReleasedSoul(Location location, ItemStack itemRepresentation) {
        location.getWorld().playSound(location, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 0.25f);
        location.getWorld().spawnParticle(Particle.SOUL, location.clone().add(0, 0.25, 0), 30,
                0.2, 0.2, 0.2, 0.05);

        Location spawnLoc = location.clone().add(0, 0.75, 0);

        location.getWorld().spawn(spawnLoc, ItemDisplay.class, display -> {
            display.setItemStack(itemRepresentation);
            display.setBillboard(Display.Billboard.FIXED);
            display.setInterpolationDuration(3);
            display.setInterpolationDelay(0);
            display.setViewRange(1.0f);

            display.setTransformationMatrix(new Matrix4f()
                    .rotateY((float) Math.toRadians(180))
                    .scale(RELEASED_HEAD_SCALE)
            );

            float[] currentRotation = {180f};
            double[] currentHeight = {0.0};
            int[] tickCounter = {0};

            Bukkit.getScheduler().runTaskTimer(
                    SoulSnatcher.getPlugin(),
                    animTask -> {
                        if (!display.isValid()) {
                            animTask.cancel();
                            return;
                        }

                        tickCounter[0]++;

                        currentRotation[0] = (currentRotation[0] + RELEASED_ROTATION_SPEED) % 360f;
                        currentHeight[0] += RELEASED_FLOAT_SPEED;

                        display.setInterpolationDelay(-1);
                        display.setTransformationMatrix(new Matrix4f()
                                .translate(0f, (float) currentHeight[0], 0f)
                                .rotateY((float) Math.toRadians(currentRotation[0]))
                                .scale(RELEASED_HEAD_SCALE)
                        );

                        if (tickCounter[0] % RELEASED_PARTICLE_INTERVAL == 0) {
                            Location current = display.getLocation().clone()
                                    .add(0, currentHeight[0] - 0.1, 0);

                            display.getWorld().spawnParticle(
                                    Particle.SOUL, current,
                                    2, 0.05, 0.05, 0.05, 0.01
                            );
                            display.getWorld().spawnParticle(
                                    Particle.SOUL_FIRE_FLAME, current,
                                    1, 0.03, 0.03, 0.03, 0.005
                            );
                        }
                    },
                    0L, 1L
            );


            SoulSnatcher.getPlugin().registerDelayedTask(() -> {
                Location finalPos = display.getLocation().clone()
                        .add(0, currentHeight[0], 0);

                display.remove();

                display.getWorld().spawnParticle(Particle.WHITE_SMOKE, finalPos, 15, 0.2, 0.2, 0.2, 0.02);
                display.getWorld().playSound(finalPos, Sound.ENTITY_CHICKEN_EGG, 1f, 0.2f
                );
            }, RELEASED_LIFETIME_TICKS);
        });
    }

    //endregion

    //region Soul Orbit

    /**
     * Adds one more soul to the orbit around the target entity.
     * If no orbit exists yet, a new task is started.
     * If one already exists, the new display is simply appended and
     * the angular offsets rebalance automatically on the next tick.
     */
    public static void addSoulToOrbit(Entity target, SoulType soulType) {
        var plugin = SoulSnatcher.getPlugin();

        double orbitRadius = target.getWidth() > ORBIT_RADIUS ? target.getWidth() * 1.1 : ORBIT_RADIUS;
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
                boolean hidden = false;

                @Override
                public void run() {
                    OrbitState current = activeOrbits.get(target.getUniqueId());

                    // Stop if the entity is gone or all displays were removed
                    if (!target.isValid() || target.isDead() || current == null) {
                        stopAllSoulOrbits(target);
                        cancel();
                        return;
                    }

                    if (target instanceof LivingEntity livingEntity && (livingEntity.isInvisible() || livingEntity.hasPotionEffect(PotionEffectType.INVISIBILITY))) {
                        if (!hidden) {
                            current.displays.forEach(display -> {
                                display.setVisibleByDefault(false);
                                Bukkit.getOnlinePlayers().stream()
                                        .filter(player -> !player.equals(target))
                                        .forEach(player -> player.hideEntity(SoulSnatcher.getPlugin(), display));
                            });
                            hidden = true;
                        }
                        return;
                    }

                    if (hidden) {
                        current.displays.forEach(display -> {
                            display.setVisibleByDefault(true);
                            if (target instanceof Player player)
                                player.hideEntity(SoulSnatcher.getPlugin(), display);
                        });

                        hidden = false;
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
                        Location headPos = orbitPosition(center, soulAngle, orbitRadius);

                        display.setInterpolationDelay(-1);
                        display.teleport(headPos);

                        if (particleTick)
                            spawnTrailParticles(current, center, soulAngle, angleStep, orbitRadius);
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
            if (!stateSoul.equals(soulType)) continue;

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
     *
     * @param player The player who will be able to see their own orbit
     */
    public static void showSoulOrbits(Player player) {
        OrbitState state = activeOrbits.getOrDefault(player.getUniqueId(), null);
        if (state == null) return;

        playerViewingOrbits.add(player.getUniqueId());
        state.displays.forEach(display -> player.showEntity(SoulSnatcher.getPlugin(), display));

        player.playSound(player, Sound.ITEM_FIRECHARGE_USE, 1f, 1.5f);
    }

    /**
     * Makes the soul orbits of the player go into hiding again
     *
     * @param player The player who hides his soul orbits from themselves
     */
    public static void hideSoulOrbits(Player player) {
        OrbitState state = activeOrbits.getOrDefault(player.getUniqueId(), null);
        if (state == null) return;

        playerViewingOrbits.remove(player.getUniqueId());
        state.displays.stream().filter(player::canSee).forEach(display -> {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (display.getTrackedBy().contains(player)) {
                        player.hideEntity(SoulSnatcher.getPlugin(), display);
                        cancel();
                        return;
                    }
                }
            }.runTaskTimer(SoulSnatcher.getPlugin(), 0L, 1L);
        });

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
                    .scale(target.getHeight() > 3 ? (float) target.getHeight() * 0.5f : HEAD_SCALE)
                    .rotateY((float) Math.toRadians(180)));

            if (target instanceof Player player) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (d.getTrackedBy().contains(player)) {
                            player.hideEntity(SoulSnatcher.getPlugin(), d);
                            cancel();
                        }
                    }
                }.runTaskTimer(SoulSnatcher.getPlugin(), 0L, 1L);
            }
        });
    }

    private static Location orbitPosition(Location center, double angleDegrees) {
        return orbitPosition(center, angleDegrees, ORBIT_RADIUS);
    }

    private static Location orbitPosition(Location center, double angleDegrees, double radius) {
        double radians = Math.toRadians(angleDegrees);
        double x = center.getX() + radius * Math.cos(radians);
        double z = center.getZ() + radius * Math.sin(radians);
        return new Location(center.getWorld(), x, center.getY(), z);
    }

    private static void spawnTrailParticles(OrbitState state, Location center, double currentAngle, double angleStep) {
        spawnTrailParticles(state, center, currentAngle, angleStep, ORBIT_RADIUS);
    }

    private static void spawnTrailParticles(OrbitState state, Location center, double currentAngle, double angleStep, double radius) {
        Location trailPos = orbitPosition(center, currentAngle - (angleStep * 2), radius);
        Location headPos = orbitPosition(center, currentAngle, radius);

        boolean isPlayer = Bukkit.getPlayer(state.owner) != null && !playerViewingOrbits.contains(state.owner);

        center.getWorld().getPlayersSeeingChunk(center.getChunk()).forEach(player -> {
            if (isPlayer && player.getUniqueId().equals(state.owner)) return;

            player.spawnParticle(Particle.SOUL, trailPos,
                    1, 0.05, 0.05, 0.05, 0.01);
            player.spawnParticle(Particle.SOUL_FIRE_FLAME, headPos,
                    1, 0.03, 0.03, 0.03, 0.005);
        });
    }

    //endregion

    //region Soul Reward

    public static void playBindEffect(Player player, SoulType soulType, Location rewardLoc) {
        rewardLoc.add(0, 1, 0);
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.5f);

        ItemDisplay display = world.spawn(rewardLoc.toCenterLocation(), ItemDisplay.class);
        display.setItemStack(soulType.itemRepresentation());
        display.setBillboard(Display.Billboard.VERTICAL);

        display.setTransformation(
                new Transformation(
                        new Vector3f(),
                        new AxisAngle4f((float) Math.PI, 0, 1, 0),
                        new Vector3f(1f, 1f, 1f),
                        new AxisAngle4f()
                )
        );

        display.setInterpolationDuration(2);

        var animationTask = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                float scale = 1f + tick * 0.1f;

                display.setTransformation(
                        new Transformation(
                                new Vector3f(0, tick * 0.015f, 0),
                                new AxisAngle4f((float) Math.PI, 0, 1, 0),
                                new Vector3f(scale, scale, scale),
                                new AxisAngle4f()
                        )
                );

                tick++;
            }
        }.runTaskTimer(SoulSnatcher.getPlugin(), 0L, 1L);

        SoulSnatcher.getPlugin().registerDelayedTask(() -> {
            display.remove();

            player.setVelocity(player.getVelocity().add(new Vector(0, 0.28, 0)));

            world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.8f);
            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.6f);

            Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(),
                    () -> world.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.6f),
                    10L);

            Location effectLoc = player.getLocation().add(0, 1, 0);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, effectLoc, 45, .25, .25, .25, .12);
            world.spawnParticle(Particle.SOUL, effectLoc, 30, .3, .3, .3, .05);

            animationTask.cancel();
        }, 15);
    }

    public static void discardSoulRewardEffect(Location location) {
        Location effectLoc = location.clone().add(0, 1, 0);

        effectLoc.getWorld().playSound(effectLoc, Sound.ENTITY_CHICKEN_EGG, 1f, 0.1f);
        effectLoc.getWorld().playSound(effectLoc, Sound.BLOCK_LAVA_EXTINGUISH, 1f, 0.1f);
        effectLoc.getWorld().spawnParticle(Particle.SOUL, effectLoc, 50, 0.3, 0, 0.3, 0.1);
    }

    public static void playBossRewardAnimation(Player owner, SoulType soulType, Location rewardLoc) {
        if (soulType.entityType() == EntityType.ENDER_DRAGON) {
            DragonBattle dragonBattle = rewardLoc.getWorld().getEnderDragonBattle();

            if (dragonBattle != null && dragonBattle.getEndPortalLocation() != null && dragonBattle.getEnderDragon() != null){
                rewardLoc = dragonBattle.getEndPortalLocation().clone().toCenterLocation().add(0, 4.5, 0);

                Location finalRewardLoc1 = rewardLoc;
                dragonBattle.getEnderDragon().getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(),
                        _ -> {
                        }, () -> {
                            consumeBossRewardAnimation(owner, soulType, finalRewardLoc1);
                        }, 1, 1);
                return;
            }
        }

        Location finalRewardLoc = rewardLoc;
        Bukkit.getServer().getGlobalRegionScheduler().runDelayed(SoulSnatcher.getPlugin(), _ -> {
            consumeBossRewardAnimation(owner, soulType, finalRewardLoc);
        }, 10);
    }

    private static void consumeBossRewardAnimation(Player owner, SoulType soulType, Location rewardLoc) {
        Location location = rewardLoc.clone().add(0, 1, 0);

        owner.getWorld().playSound(location, Sound.ENTITY_WITHER_AMBIENT, 1f, 0.2f);

        for (int i = 0; i < 250; i++) {
            double x = Math.random() * 4 - 2;
            double y = Math.random() * 2 - 1;
            double z = Math.random() * 4 - 2;

            Particle.PORTAL.builder()
                    .location(location)
                    .offset(x, y, z)
                    .count(0)
                    .receivers(32, true)
                    .spawn();
        }

        Bukkit.getServer().getGlobalRegionScheduler().runDelayed(SoulSnatcher.getPlugin(), _ -> {
            Particle.REVERSE_PORTAL.builder()
                    .location(location)
                    .offset(0.7, 0.7, 0.7)
                    .count(250)
                    .receivers(32, true)
                    .spawn();
            location.getWorld().playSound(location, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 1.85f);

            SoulReward.offerSoulReward(rewardLoc, owner, soulType);
        }, 50);
    }

    //endregion
}
