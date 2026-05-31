package at.gaderman.soulSnatcher.mobGoals;

import at.gaderman.soulSnatcher.SoulSnatcher;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.EnumSet;

/**
 * A custom behaviour mob goal that adds an ability to mobs. While pursuing a target wil spawn in a bow
 * who will after some loading time make the mob shoot an arrow at facing direction. This ability has a small
 * cooldown which will be periodically activated as long as the mob has a valid target.
 */
public class GhastShootGoal extends SoulAbilityGoal {

    private final int shotCooldown;

    public GhastShootGoal(Mob mob, int shotCooldown){
        super(mob);

        this.shotCooldown = shotCooldown;
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "infused_ghast_shoot"));

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GOAL_KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }

    private long lastShot;

    @Override
    public boolean shouldActivate() {
        return mob.getTarget() != null && lastShot < System.currentTimeMillis() - shotCooldown;
    }

    private long preparingTicks;
    private static final long PREPARE_TILL_SHOOT = 20L;

    @Override
    public boolean shouldStayActive() {
        return preparingTicks <= PREPARE_TILL_SHOOT;
    }

    private ItemDisplay fireball;

    @Override
    public void start() {
        Location fireballSource = mob.getLocation().add(0, mob.getEyeHeight(true) + 0.5, 0);
        fireballSource.getWorld().spawnParticle(Particle.WHITE_SMOKE, fireballSource, 5, 0, 0, 0, 0.1);
        fireballSource.getWorld().playSound(fireballSource, Sound.ENTITY_GHAST_SCREAM, 2f, 0.2f);

        fireball = mob.getWorld().spawn(fireballSource, ItemDisplay.class, display -> {
            display.setItemStack(ItemStack.of(Material.FIRE_CHARGE));
            display.setBillboard(Display.Billboard.FIXED);

            display.setTransformation(
                    new Transformation(
                            new Vector3f(0, 0.25f, 0),
                            new AxisAngle4f(),
                            new Vector3f(0.9f, 0.9f, 0.9f),
                            new AxisAngle4f()
                    )
            );
        });
        mob.addPassenger(fireball);

        SoulSnatcher.getPlugin().registerDelayedTask(fireball::remove, 2 * PREPARE_TILL_SHOOT + 1);
    }

    @Override
    public void tick() {
        preparingTicks++;

        fireball.getWorld().spawnParticle(Particle.FLAME, fireball.getLocation(), 1, 0, 0, 0, 0.01);
    }

    @Override
    public void stop() {
        preparingTicks = 0;
        lastShot = System.currentTimeMillis();

        mob.getWorld().playSound(mob, Sound.ENTITY_GHAST_SHOOT, 2f, 1f);
        mob.launchProjectile(LargeFireball.class, mob.getLocation().getDirection().multiply(2));
    }

}
